package ninesstack.nmesos.commands

import ninesstack.nmesos.docker.SshDockerClient
import ninesstack.nmesos.docker.model.Container
import ninesstack.nmesos.sidecar._
import ninesstack.nmesos.singularity.DryrunSingularityManager
import ninesstack.nmesos.singularity.model.SingularityRequestParent
import ninesstack.nmesos.util.Formatter

import scala.Option.option2Iterable
import scala.util.{Failure, Success}

/**
  * Verify a complete Cluster comparing Singularity, Mesos, Docker and Sidecar State.
  */
case class VerifyEnvCommand(singularityUrl: String, fmt: Formatter)
    extends Command
    with VerifyRequests
    with VerifyContainers
    with VerifySidecar
    with FetchEnvironment {

  private val logger = org.log4s.getLogger

  def run(): CommandResult = {
    logger.info(s"singularityUrl: ${singularityUrl}")

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
        CommandError(s"Unable to connect - ${ex}")
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
  def fmt: Formatter

  var manager = DryrunSingularityManager(singularityUrl, fmt)
  var sidecar = SidecarManager(fmt)

  def fetchInfo() =
    fmt.fmtBlock(s"Analyzing: $singularityUrl") {
      for {
        requests <- manager.getSingularityActiveRequests()
        hosts =
          requests
            .flatMap(_.taskIds)
            .flatMap(_.healthy)
            .map(_.sanitizedHost)
            .distinct
            .map(desanitized)
        containers = hosts.map(fetchContainers).flatten
        sidecars = fetchSidecarInfo(containers)
        info = EnvironmentInfo(requests, containers, sidecars)
      } yield info
    }

  private def fetchContainers(host: String): Seq[Container] = {
    fmt.println(s"Fetching Docker containers from ${host}...")
    SshDockerClient.fetchContainers(host)
  }

  /**
    * Recover Sidecar info if Sidecar container pressent
    */
  private def fetchSidecarInfo(
      containers: Seq[Container]
  ): Seq[SidecarServices] = {
    val sidecarHosts = containers
      .filter(_.image.contains("gonitro/sidecar"))
      .map(_.host)
      .distinct
      .sorted
    sidecarHosts.map(sidecar.getServices)
      .flatMap(_.toOption.flatten.toSeq)
  }

  private def desanitized(sanitizedHost: String) =
    sanitizedHost.replaceAll("_", "-")

}

trait VerifyContainers {
  def fmt: Formatter

  /**
    * Verify all containers running in the cluster belong to a Singularity Requests.
    */
  def verifyOrphanContainers(info: EnvironmentInfo) = {
    fmt.fmtBlock(s"Verifying containers") {

      val mesosTasksId = info.requests
        .flatMap(_.taskIds)
        .flatMap(_.healthy)
        .map(_.id)

      val runningContainerTaskId =
        info.containers.filter(_.name.startsWith("mesos-")).map(_.taskId)
      val orphans = runningContainerTaskId.diff(mesosTasksId)

      print(info, mesosTasksId, orphans)

      orphans.isEmpty
    }
  }

  private def print(
      info: EnvironmentInfo,
      mesosTasksId: Seq[String],
      orphans: Seq[String]
  ) = {
    if (orphans.isEmpty) {
      fmt.println(
        s" ${fmt.Ok} All containers belong to a Mesos Task [${info.containers.length} containers, ${mesosTasksId.length} tasks]"
      )
    } else {
      fmt.error(s" Probably orphan containers:")

      orphans.foreach { taskId =>
        val containerInfo = info.containers
          .find(_.taskId == taskId)
          .fold(s"$taskId")(c =>
            s"   at ${c.host} [containerId: ${c.id}, image: ${c.image}, name: ${c.name}]"
          )
        fmt.error(s" ${fmt.Fail} $containerInfo")
      }
    }
  }
}

trait VerifyRequests {
  def fmt: Formatter

  /**
    * Verify all requests
    */
  def verifyRequests(info: EnvironmentInfo) = {
    fmt.fmtBlock(s"Verifying requests") {
      info.requests
        .map(verifyRequest(_, info))
        .forall(identity)
    }
  }

  /**
    * Verify all task for the given request have a container running and no orphan containers are found.
    */
  def verifyRequest(
      request: SingularityRequestParent,
      info: EnvironmentInfo
  ): Boolean = {
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

    printResult(
      tasksCount,
      request.request.id,
      orphanContainers,
      foundContainers
    )

    orphanContainers.isEmpty
  }

  /**
    * Print to std the output result
    */
  def printResult(
      tasksCount: Int,
      requestId: String,
      orphanContainers: Seq[Container],
      foundContainers: Seq[Container]
  ) = {

    if (!orphanContainers.isEmpty) {
      val orphasInfo = orphanContainers
        .map(c => s" at ${c.host} [containerId: ${c.id}, name: ${c.name}]")
        .mkString("\n\t\t\t")

      val validInfo = foundContainers
        .map(c => s" at ${c.host} [containerId: ${c.id}]")
        .mkString("\n\t\t\t")

      fmt.error(s""" ${fmt.Fail} $requestId: $tasksCount tasks, ${fmt
        .errorColor(foundContainers.length)} containers found, ${fmt.errorColor(
        orphanContainers.length
      )} unexpected containers
           |  orphans:\t${fmt.errorColor(orphasInfo)}
           |  valid:\t${fmt.infoColor(validInfo)}
        """.stripMargin)
    } else {
      fmt.println(
        s" ${fmt.Ok} $requestId: $tasksCount tasks, ${fmt.infoColor(foundContainers.length)} containers found"
      )
    }
  }
}

trait VerifySidecar {
  implicit val fmt: Formatter

  def verifySidecar(info: EnvironmentInfo): Boolean = {

    if (info.sidecarsServices.nonEmpty) {
      fmt.fmtBlock(s"Verifying Sidecar") {
        val allHost = info.containers.map(_.host).distinct.sorted
        val sidecarHosts = info.sidecarsServices.map(_.hostName).distinct.sorted

        // All host are running sidecar
        if (allHost == sidecarHosts) {
          fmt.println(
            s" ${fmt.Ok} ${sidecarHosts.size} Sidecar instances running "
          )
        } else {
          fmt.println(
            s" ${fmt.Fail} ${sidecarHosts.size} Sidecar instances running [${allHost.size} expected]"
          )
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
