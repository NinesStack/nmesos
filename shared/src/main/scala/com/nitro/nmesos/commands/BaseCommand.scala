package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.SingularityManager
import com.nitro.nmesos.singularity.model.SingularityRequestParent.SingularityActiveDeployResponse
import com.nitro.nmesos.singularity.model.{
  SingularityRequest,
  SingularityResources,
  SingularityUpdateResult
}
import com.nitro.nmesos.util.Logger

import scala.util.{Failure, Success, Try}

sealed trait CommandResult
case class CommandSuccess(msg: String) extends CommandResult
case class CommandError(msg: String) extends CommandResult

trait Command {

  /**
    * Main entry point for all the commands.
    */
  def run(): CommandResult
}

trait BaseCommand extends Command {
  val localConfig: CmdConfig
  val log: Logger
  val isDryrun: Boolean

  val manager = SingularityManager(
    localConfig.environment.singularity,
    log,
    isDryrun = isDryrun
  )

  /**
    * Concrete Command implementation
    */
  protected def processCmd(): CommandResult

  /**
    * Main entry point for all the commands.
    */
  def run(): CommandResult = {

    showCommand()

    // Check visibility before attempting to process the command.
    manager.ping() match {
      case Failure(_) =>
        CommandError(
          s"Unable to connect to ${localConfig.environment.singularity.url}"
        )
      case Success(_) =>
        processCmd()
    }

  }

  protected def showCommand() = {
    log.logBlock("Deploying Config") {
      log.info(
        s""" Service Name: ${localConfig.serviceName}
           | Config File:  ${localConfig.file.getAbsolutePath}
           | environment:  ${localConfig.environmentName}
           | dry-run:      ${isDryrun}
           | force:        ${localConfig.force}
           | image:        ${localConfig.environment.container.image}:${localConfig.tag}
           | api:          ${localConfig.environment.singularity.url}""".stripMargin
      )
    }
  }

  def getRemoteRequest(
      localRequest: SingularityRequest
  ): Try[Option[SingularityRequest]] = {
    log.debug("Fetching the remote request configuration...")
    manager.getSingularityRequest(localRequest.id).map(_.map(_.request))
  }

  def getActiveDeploy(
      localRequest: SingularityRequest
  ): Try[Option[SingularityActiveDeployResponse]] = {
    log.debug("Fetching the remote active deploy...")
    manager
      .getSingularityRequest(localRequest.id)
      .map(_.flatMap(_.activeDeploy))
  }

  /**
    * Create or update the Singularity request based on the diff between local and remote.
    */
  def updateSingularityRequestIfNeeded(
      remoteOpt: Option[SingularityRequest],
      local: SingularityRequest
  ) = {
    log.debug("Comparing remote and local configuration...")
    remoteOpt match {
      case None =>
        log.info(s" No Mesos config found with id: '${local.id}'")
        manager.createSingularityRequest(local).map(_ => local)

      case Some(remote) if (remote != local) =>
        // Remote Singularity request exist but need to be updated.
        manager.updateSingularityRequest(remote, local).map(_ => local)

      case Some(other) =>
        // Remote Singularity Request is up to date, nothing to do here!
        Success(
          log.info(
            s" The request configuration for '${localConfig.serviceName}' is up to date! [requestId: ${other.id}]"
          )
        )
        Success(local)
    }
  }

  /**
    * Scale the resources of the Singularity request based on the diff between local and remote.
    * Note: Only the number of instances will be updated.
    */
  def scaleSingularityRequestIfNeeded(
      remoteOpt: Option[SingularityRequest],
      local: SingularityRequest
  ): Try[SingularityRequest] = {

    log.debug("Comparing remote and local configuration...")

    remoteOpt match {
      case None =>
        Failure(new Exception(s" No Mesos config found with id: '${local.id}'"))

      case Some(SingularityRequest(_, _, "SPREAD_ALL_SLAVES", _, _, _, _, _)) =>
        Failure(
          new Exception(
            s" Unable to scale to a fix number of instances. Using auto-scale [slavePlacement: SPREAD_ALL_SLAVES]"
          )
        )

      case Some(remote) if (remote.instances != local.instances) =>
        // Remote Singularity request exist but num instances need to be scaled
        manager.scaleSingularityRequest(remote, local).map(_ => local)

      case Some(other) =>
        // Remote Singularity Request is up to date, nothing to do here!
        Success(
          log.info(
            s" The request configuration for '${localConfig.serviceName}' is up to date! [requestId: ${other.id}]"
          )
        )
        Success(local)
    }
  }

  def dryWarning =
    if (isDryrun) log.importantColor(" [dry-run true] use --dry-run false")
    else ""

}
