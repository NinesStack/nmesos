package com.nitro.nmesos.commands

import com.nitro.nmesos.docker.SshDockerClient
import com.nitro.nmesos.docker.model.Container
import com.nitro.nmesos.sidecar._
import com.nitro.nmesos.singularity.DryrunSingularityManager
import com.nitro.nmesos.singularity.model.SingularityRequestParent
import com.nitro.nmesos.util.Logger

import scala.Option.option2Iterable
import scala.util.{ Failure, Success }

/**
 * Verify a complete Cluster comparing Singularity, Mesos, Docker and Sidecar State.
 */
case class VerifyEnvCommand(singularityUrl: String, log: Logger) extends Command
  with VerifyRequests
  with VerifyContainers
  with VerifySidecar
  with FetchEnvironment {

  def run(): CommandResult = {

    val tryVerify = for {
      info <- fetchInfo()
    } yield {
      val requests = verifyRequests(info)
      val containers = verifyOrphanContainers(info)
      val sidecar = verifySidecar(info)
      requests && containers && sidecar
    }

    tryVerify match {
      case Failure(ex) =>
        CommandError(s"Unable to connect - ${ex.getMessage}")
      case Success(true) =>
        CommandSuccess(s"Successfully Verified")
      case Success(false) =>
        CommandError("Errors detected")
    }
  }

}

/**
 * Gather all needed info from Containers, Sidecar and Singularity.
 */
trait FetchEnvironment {
  def singularityUrl: String
  def log: Logger

  var manager = DryrunSingularityManager(singularityUrl, log)
  var sidecar = SidecarManager(log)

  def fetchInfo() = log.logBlock(s"Analyzing: $singularityUrl") {
    for {
      requests <- manager.getSingularityActiveRequests()
      hosts = requests.flatMap(_.taskIds).flatMap(_.healthy).map(_.sanitizedHost).distinct.map(desanitized)
      containers = hosts.par.map(fetchContainers).seq.flatten
      sidecars = fetchSidecarInfo(containers)
      info = EnvironmentInfo(requests, containers, sidecars)
    } yield info
  }

  private def fetchContainers(host: String): Seq[Container] = {
    log.println(s"Fetching Docker container from $host...")
    SshDockerClient.fetchContainers(host)
  }.seq

  /**
   * Recover Sidecar info if Sidecar container pressent
   */
  private def fetchSidecarInfo(containers: Seq[Container]): Seq[SidecarServices] = {
    val sidecarHosts = containers.filter(_.image.contains("gonitro/sidecar")).map(_.host).distinct.sorted
    sidecarHosts.map(sidecar.getServices).flatMap(_.toOption.flatten.toSeq)
  }

  private def desanitized(sanitizedHost: String) = sanitizedHost.replaceAll("_", "-")

}

trait VerifyContainers {
  def log: Logger

  /**
   * Verify all containers running in the cluster belong to a Singularity Requests.
   */
  def verifyOrphanContainers(info: EnvironmentInfo) = {
    log.logBlock(s"Verifying containers") {

      val mesosTasksId = info.requests
        .flatMap(_.taskIds)
        .flatMap(_.healthy)
        .map(_.id)

      val runningContainerTaskId = info.containers.filter(_.name.startsWith("mesos-")).map(_.taskId)
      val orphans = runningContainerTaskId.diff(mesosTasksId)

      print(info, mesosTasksId, orphans)

      orphans.isEmpty
    }
  }

  private def print(info: EnvironmentInfo, mesosTasksId: Seq[String], orphans: Seq[String]) = {
    if (orphans.isEmpty) {
      log.println(s" ${log.Ok} All containers belong to a Mesos Task [${info.containers.length} containers, ${mesosTasksId.length} tasks]")
    } else {
      log.error(s" Probably orphan containers:")

      orphans.foreach { taskId =>
        val containerInfo = info.containers.find(_.taskId == taskId)
          .fold(s"$taskId")(c => s"   at ${c.host} [containerId: ${c.id}, image: ${c.image}, name: ${c.name}]")
        log.error(s" ${log.Fail} $containerInfo")
      }
    }
  }
}

trait VerifyRequests {
  def log: Logger

  /**
   * Verify all requests
   */
  def verifyRequests(info: EnvironmentInfo) = {
    log.logBlock(s"Verifying requests") {
      info.requests.map(verifyRequest(_, info))
        .forall(identity)
    }
  }

  /**
   * Verify all task for the given request have a container running and no orphan containers are found.
   */
  def verifyRequest(request: SingularityRequestParent, info: EnvironmentInfo): Boolean = {
    val tasksCount = request.taskIds.toSeq.flatMap(_.healthy).length

    val containersByRequest = info.containers
      .filter(_.env.get("TASK_REQUEST_ID").exists(_ == request.request.id))

    val singularityTasksId = request.taskIds.toSeq
      .flatMap(_.healthy)
      .map(_.id)

    val foundContainers = containersByRequest.filter { container =>
      singularityTasksId.exists(_ == container.taskId)
    }

    val orphanContainers = containersByRequest.diff(foundContainers)

    printResult(tasksCount, request.request.id, orphanContainers, foundContainers)

    orphanContainers.isEmpty
  }

  /**
   * Print to std the output result
   */
  def printResult(tasksCount: Int, requestId: String, orphanContainers: Seq[Container], foundContainers: Seq[Container]) = {

    if (!orphanContainers.isEmpty) {
      val orphasInfo = orphanContainers.map(c => s" at ${c.host} [containerId: ${c.id}, name: ${c.name}]")
        .mkString("\n\t\t\t")

      val validInfo = foundContainers.map(c => s" at ${c.host} [containerId: ${c.id}]")
        .mkString("\n\t\t\t")

      log.error(
        s""" ${log.Fail} $requestId: $tasksCount tasks, ${log.errorColor(foundContainers.length)} containers found, ${log.errorColor(orphanContainers.length)} unexpected containers
           |  orphans:\t${log.errorColor(orphasInfo)}
           |  valid:\t${log.infoColor(validInfo)}
        """.stripMargin)
    } else {
      log.println(
        s" ${log.Ok} $requestId: $tasksCount tasks, ${log.infoColor(foundContainers.length)} containers found")
    }
  }
}

trait VerifySidecar {
  implicit val log: Logger

  def verifySidecar(info: EnvironmentInfo): Boolean = {

    if (info.sidecarsServices.nonEmpty) {
      log.logBlock(s"Verifying Sidecar") {
        val allHost = info.containers.map(_.host).distinct.sorted
        val sidecarHosts = info.sidecarsServices.map(_.hostName).distinct.sorted

        // All host are running sidecar
        if (allHost == sidecarHosts) {
          log.println(s" ${log.Ok} ${sidecarHosts.size} Sidecar instances running ")
        } else {
          log.println(s" ${log.Fail} ${sidecarHosts.size} Sidecar instances running [${allHost.size} expected]")
        }

        val inSync = SidecarUtils.verifyInSync(info)
        val services = SidecarUtils.verifyServices(info)
        inSync && services
      }
    } else {
      true // No sidecars running (assuming cluster valid)
    }
  }
}
