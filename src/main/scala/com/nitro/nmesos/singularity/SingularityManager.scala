package com.nitro.nmesos.singularity

import com.nitro.nmesos.config.model.SingularityConf
import com.nitro.nmesos.singularity.ModelConversions.DeployId
import com.nitro.nmesos.singularity.model._
import com.nitro.nmesos.util.{HttpClientHelper, InfoFormatter, Formatter}
import scala.util.{Success, Try}
import com.nitro.nmesos.util.Conversions._

/**
  * Can build Dryrun (readonly) or Real Singularity Manager (read-write).
  */
object SingularityManager {
  def apply(
      conf: SingularityConf,
      fmt: Formatter,
      isDryrun: Boolean
  ): SingularityManager =
    if (isDryrun) {
      DryrunSingularityManager(conf.url, fmt)
    } else {
      RealSingularityManager(conf, fmt)
    }
}

trait SingularityManager extends HttpClientHelper {
  private val logger = org.log4s.getLogger

  val apiUrl: String

  def createSingularityRequest(
      newRequest: SingularityRequest
  ): Try[SingularityRequestParent]

  def scaleSingularityRequest(
      previous: SingularityRequest,
      request: SingularityRequest
  ): Try[SingularityUpdateResult]

  def updateSingularityRequest(
      previous: SingularityRequest,
      request: SingularityRequest
  ): Try[SingularityUpdateResult]

  def deploySingularityDeploy(
      currentRequest: SingularityRequest,
      newDeploy: SingularityDeploy,
      message: String
  ): Try[SingularityRequestParent]

  def ping(): Try[Unit] = {
    logger.info(apiUrl)
    get[Unit](s"$apiUrl/api/requests").map(_.getOrElse(()))
  }

  def getSingularityRequest(
      requestId: String
  ): Try[Option[SingularityRequestParent]] = {
    get[SingularityRequestParent](s"$apiUrl/api/requests/request/$requestId")
  }

  def getSingularityDeployHistory(
      requestId: String,
      deployId: DeployId
  ): Try[Option[SingularityDeployHistory]] = {
    get[SingularityDeployHistory](
      s"$apiUrl/api/history/request/$requestId/deploy/$deployId"
    )
  }

  def getSingularityTaskHistory(
      requestId: String,
      deployId: DeployId
  ): Try[Seq[SingularityTaskIdHistory]] = {
    get[Seq[SingularityTaskIdHistory]](
      s"$apiUrl/api/history/request/$requestId/tasks?requestId=$requestId&deployId=$deployId"
    )
  }.map(_.getOrElse(Seq.empty))

  /**
    * Undocumented api to fetch stdout/stderr logs:
    * https://github.com/HubSpot/Singularity/issues/1309
    *
    * @param path stdout or stderr
    */
  private val DefaultLength = 50000

  def getLogs(taskId: String, path: String): Try[Seq[String]] =
    for {
      logOp <- get[SingularityLog](
        s"$apiUrl/api/sandbox/$taskId/read?path=$path&length=$DefaultLength&offset=0"
      )
      log <- logOp.toTry("Logs not found")
    } yield {
      log.data.split("\n").toSeq
    }

  def getSingularityActiveTasks(): Try[Seq[SingularityTask]] = {
    get[Seq[SingularityTask]](s"$apiUrl/api/tasks/active")
      .map(_.getOrElse(Seq.empty))
  }

  def getActiveTasks(request: SingularityRequest): Try[Seq[SingularityTask]] = {
    for {
      allTasks <- getSingularityActiveTasks()
    } yield {
      allTasks
        .filter(task => task.taskId.requestId == request.id)
        .sortBy(_.taskId.id) // predictable order
    }
  }

  def getSingularityPendingDeploy(): Try[Seq[SingularityPendingDeploy]] = {
    get[Seq[SingularityPendingDeploy]](s"$apiUrl/api/deploys/pending")
      .map(_.getOrElse(Seq.empty))
  }

  def getSingularityPendingDeploy(
      requestId: String,
      deployId: DeployId
  ): Try[Option[SingularityPendingDeploy]] = {
    getSingularityPendingDeploy().map { allPending =>
      allPending
        .filter(_.deployMarker.deployId == deployId)
        .filter(_.deployMarker.requestId == requestId)
        .headOption
    }
  }

  // Retrieve the list of active requests
  def getSingularityActiveRequests(): Try[Seq[SingularityRequestParent]] = {
    get[Seq[SingularityRequestParent]](
      s"$apiUrl/api/requests/active?includeFullRequestData=true"
    ).map(_.getOrElse(Seq.empty))
  }

  def withDisabledDebugFmt(): SingularityManager

}

/**
  * Singularity Manager in Dryrun mode.
  */
case class DryrunSingularityManager(apiUrl: String, fmt: Formatter)
    extends SingularityManager {

  def createSingularityRequest(newRequest: SingularityRequest) = {
    if (newRequest.schedule.isDefined) {
      fmt.info(
        s" [dryrun] Need to schedule a new Mesos Job with id: ${newRequest.id}"
      )
    } else {
      fmt.info(
        s" [dryrun] Need to create a new Mesos service with id: ${newRequest.id}, instances: ${newRequest.instances
          .getOrElse("0")}"
      )
    }
    Success(SingularityRequestParent(request = newRequest, state = "ACTIVE"))
  }

  def scaleSingularityRequest(
      previous: SingularityRequest,
      request: SingularityRequest
  ) = {
    fmt.info(s""" [dryrun] Need to scale from ${previous.instances.getOrElse(
      "0"
    )} to ${request.instances.getOrElse("0")} instances""")
    Success(SingularityUpdateResult(state = "ACTIVE"))
  }

  def updateSingularityRequest(
      previous: SingularityRequest,
      request: SingularityRequest
  ) = {
    fmt.info(s" [dryrun] Need to update ${request.id}")
    if (previous.schedule != request.schedule) {
      fmt.info(s""" [dryrun] Need to reschedule '${previous.schedule.getOrElse(
        ""
      )}' to '${previous.schedule.getOrElse("")}'""")
    }

    Success(SingularityUpdateResult(state = "ACTIVE"))
  }

  def deploySingularityDeploy(
      currentRequest: SingularityRequest,
      newDeploy: SingularityDeploy,
      message: String
  ): Try[SingularityRequestParent] = {
    val message =
      s" [dryrun] Need to deploy image '${newDeploy.containerInfo.docker.image}'"
    fmt.info(message)
    val detail = ModelConversions.describeDeploy(currentRequest, newDeploy)
    fmt.info(s""" [dryrun] Deploy to apply:
         |${detail.mkString(
      "           * ",
      "\n           * ",
      ""
    )}""".stripMargin)

    Success(SingularityRequestParent(currentRequest, state = "ACTIVE"))
  }

  def withDisabledDebugFmt(): SingularityManager = this.copy(fmt = InfoFormatter)
}

/**
  * Singularity Manager with All write operations.
  */
case class RealSingularityManager(conf: SingularityConf, fmt: Formatter)
    extends SingularityManager {
  val apiUrl: String = conf.url

  def createSingularityRequest(newRequest: SingularityRequest) = {
    fmt.debug("Creating Singularity Request...")
    val response = post[SingularityRequest, SingularityRequestParent](
      s"$apiUrl/api/requests",
      newRequest
    )

    response.foreach {
      case response if (newRequest.schedule.isEmpty) =>
        fmt.info(
          s""" Created new Mesos service with Id: ${newRequest.id}, instances: ${newRequest.instances
            .getOrElse("0")}, state: ${response.state}"""
        )
      case response if (newRequest.schedule.isDefined) =>
        fmt.info(
          s""" Scheduled new Mesos job with Id: ${newRequest.id}, state: ${response.state}"""
        )
    }

    response
  }

  def scaleSingularityRequest(
      previous: SingularityRequest,
      request: SingularityRequest
  ) = {
    val message =
      s""" Scaling '${request.id}' from ${previous.instances.getOrElse(
        "0"
      )} to ${request.instances.getOrElse("0")} instances"""
    fmt.info(message)
    val scaleRequest = SingularityScaleRequest(message, request.instances)

    val response = put[SingularityScaleRequest, SingularityUpdateResult](
      s"$apiUrl/api/requests/request/${request.id}/scale",
      scaleRequest
    )

    response.foreach {
      case response =>
        fmt.info(s" ${request.id} scaled, instances: ${request.instances
          .getOrElse("0")}, state: ${response.state}")
    }

    response
  }

  def updateSingularityRequest(
      previous: SingularityRequest,
      request: SingularityRequest
  ) = {
    fmt.info(s" Need to update ${request.id}")
    if (previous.schedule != request.schedule) {
      fmt.info(s""" Rescheduling '${request.id}' cron from '${previous.schedule
        .getOrElse("")}' to '${request.schedule.getOrElse("")}'""")
    }
    post[SingularityRequest, SingularityUpdateResult](
      s"$apiUrl/api/requests",
      request
    )
  }

  def deploySingularityDeploy(
      currentRequest: SingularityRequest,
      newDeploy: SingularityDeploy,
      message: String
  ) = {
    fmt.info(message)
    fmt.debug(
      s" [requestId: ${newDeploy.requestId}, deployId: ${newDeploy.id}]"
    )

    val deployRequest = SingularityDeployRequest(
      newDeploy,
      message,
      unpauseOnSuccessfulDeploy = true
    )

    val response = post[SingularityDeployRequest, SingularityRequestParent](
      s"$apiUrl/api/deploys",
      deployRequest
    )

    response.foreach { _ =>
      val detail = ModelConversions.describeDeploy(currentRequest, newDeploy)
      fmt.info(s""" Deploy applied:
           |${detail.mkString("   * ", "\n   * ", "")}""".stripMargin)

    }

    response
  }

  // Some behaviour debug fmts disabled.
  def withDisabledDebugFmt(): SingularityManager = {
    this.copy(fmt = InfoFormatter)
  }
}
