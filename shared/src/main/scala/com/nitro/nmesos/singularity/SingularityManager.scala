package com.nitro.nmesos.singularity

import com.nitro.nmesos.config.model.SingularityConf
import com.nitro.nmesos.singularity.ModelConversions.DeployId
import com.nitro.nmesos.singularity.model._
import com.nitro.nmesos.util.{ HttpClientHelper, InfoLogger, Logger }
import scala.util.{ Success, Try }

/**
 * Can build Dryrun (readonly) or Real Singularity Manager (read-write).
 */
object SingularityManager {
  def apply(conf: SingularityConf, log: Logger, isDryrun: Boolean): SingularityManager = if (isDryrun) {
    DryrunSingularityManager(conf, log)
  } else {
    RealSingularityManager(conf, log)
  }
}

trait SingularityManager extends HttpClientHelper {
  val apiUrl: String

  def createSingularityRequest(newRequest: SingularityRequest): Try[SingularityRequestParent]

  def scaleSingularityRequest(previous: SingularityRequest, request: SingularityRequest): Try[SingularityScaleUpResult]

  def deploySingularityDeploy(currentRequest: SingularityRequest, newDeploy: SingularityDeploy, message: String): Try[SingularityRequestParent]

  def ping(): Try[Unit] = {
    get[Unit](s"$apiUrl/api/requests").map(_.getOrElse(()))
  }

  def getSingularityRequest(requestId: String): Try[Option[SingularityRequestParent]] = {
    get[SingularityRequestParent](s"$apiUrl/api/requests/request/$requestId")
  }

  def getSingularityDeployHistory(requestId: String, deployId: DeployId): Try[Option[SingularityDeployHistory]] = {
    get[SingularityDeployHistory](s"$apiUrl/api/history/request/$requestId/deploy/$deployId")
  }

  def getSingularityTaskHistory(requestId: String): Try[Seq[SingularityTaskIdHistory]] = {
    get[Seq[SingularityTaskIdHistory]](s"$apiUrl/api/history/tasks?requestId=$requestId")
      .map(_.getOrElse(Seq.empty))
  }

  def getSingularityTaskHistory(request: SingularityRequest): Try[Seq[SingularityTaskIdHistory]] = {
    for {
      all <- getSingularityTaskHistory(request.id)
    } yield all.take(request.instances * 2) // Take at least the previous deploy history.
  }

  def getSingularityActiveTasks(): Try[Seq[SingularityTask]] = {
    get[Seq[SingularityTask]](s"$apiUrl/api/tasks/active")
      .map(_.getOrElse(Seq.empty))
  }

  def getActiveTask(request: SingularityRequest): Try[Seq[SingularityTask]] = {
    for {
      allTasks <- getSingularityActiveTasks()
    } yield {
      allTasks.filter(task => task.taskId.requestId == request.id)
        .sortBy(_.taskId.id) // predictable order
    }
  }

  def getSingularityPendingDeploy(): Try[Seq[SingularityPendingDeploy]] = {
    get[Seq[SingularityPendingDeploy]](s"$apiUrl/api/deploys/pending").map(_.getOrElse(Seq.empty))
  }

  def getSingularityPendingDeploy(requestId: String, deployId: DeployId): Try[Option[SingularityPendingDeploy]] = {
    getSingularityPendingDeploy().map { allPending =>
      allPending.filter(_.deployMarker.deployId == deployId)
        .filter(_.deployMarker.requestId == requestId)
        .headOption
    }
  }

  def withDisabledDebugLog(): SingularityManager

}

/**
 * Singularity Manager in Dryrun mode.
 */
case class DryrunSingularityManager(conf: SingularityConf, log: Logger) extends SingularityManager {
  val apiUrl: String = conf.url

  def createSingularityRequest(newRequest: SingularityRequest) = {
    log.info(s" [dryrun] Need to create a new Mesos service with id: ${newRequest.id}, instances: ${newRequest.instances}")
    Success(SingularityRequestParent(request = newRequest, state = "ACTIVE"))
  }

  def scaleSingularityRequest(previous: SingularityRequest, request: SingularityRequest) = {
    log.info(s" [dryrun] Need to scale from ${previous.instances} to ${request.instances} instances")
    Success(SingularityScaleUpResult(state = "ACTIVE"))
  }

  def deploySingularityDeploy(currentRequest: SingularityRequest, newDeploy: SingularityDeploy, message: String): Try[SingularityRequestParent] = {
    val message = s" [dryrun] Need to deploy image '${newDeploy.containerInfo.docker.image}'"
    log.info(message)
    val detail = ModelConversions.describeDeploy(currentRequest, newDeploy)
    log.info(
      s""" [dryrun] Deploy to apply:
         |${detail.mkString("           * ", "\n           * ", "")}""".stripMargin
    )

    Success(SingularityRequestParent(currentRequest, state = "ACTIVE"))
  }

  def withDisabledDebugLog(): SingularityManager = this.copy(log = InfoLogger)
}

/**
 * Singularity Manager with All write operations.
 */
case class RealSingularityManager(conf: SingularityConf, log: Logger) extends SingularityManager {
  val apiUrl: String = conf.url

  def createSingularityRequest(newRequest: SingularityRequest) = {
    log.debug("Creating Singularity Request...")
    val response = post[SingularityRequest, SingularityRequestParent](s"$apiUrl/api/requests", newRequest)

    response.foreach {
      case response =>
        log.info(s" Created new Mesos service with Id: ${newRequest.id}, instances: ${newRequest.instances}, state: ${response.state}")
    }

    response
  }

  def scaleSingularityRequest(previous: SingularityRequest, request: SingularityRequest) = {
    val message = s" Scaling '${request.id}' from ${previous.instances} to ${request.instances} instances"
    log.info(message)
    val scaleRequest = SingularityScaleRequest(message, request.instances)

    val response = put[SingularityScaleRequest, SingularityScaleUpResult](s"$apiUrl/api/requests/request/${request.id}/scale", scaleRequest)

    response.foreach {
      case response =>
        log.info(s" ${request.id} scaled, instances: ${request.instances}, state: ${response.state}")
    }

    response
  }

  def deploySingularityDeploy(currentRequest: SingularityRequest, newDeploy: SingularityDeploy, message: String) = {
    log.info(message)
    log.debug(s" [requestId: ${newDeploy.requestId}, deployId: ${newDeploy.id}]")

    val deployRequest = SingularityDeployRequest(newDeploy, message, unpauseOnSuccessfulDeploy = true)

    val response = post[SingularityDeployRequest, SingularityRequestParent](s"$apiUrl/api/deploys", deployRequest)

    response.foreach { _ =>
      val detail = ModelConversions.describeDeploy(currentRequest, newDeploy)
      log.info(
        s""" Deploy applied:
           |${detail.mkString("   * ", "\n   * ", "")}""".stripMargin
      )

    }

    response
  }

  // Some behaviour debug logs disabled.
  def withDisabledDebugLog(): SingularityManager = {
    this.copy(log = InfoLogger)
  }
}
