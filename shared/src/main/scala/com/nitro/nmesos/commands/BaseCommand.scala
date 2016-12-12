package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.SingularityManager
import com.nitro.nmesos.singularity.model.SingularityRequestParent.SingularityActiveDeployResponse
import com.nitro.nmesos.singularity.model.{ SingularityRequest, SingularityResources }
import com.nitro.nmesos.util.Logger

import scala.util.{ Failure, Success, Try }

sealed trait CommandResult

case object CommandSuccess extends CommandResult

case class CommandError(msg: String) extends CommandResult

trait BaseCommand {
  val localConfig: CmdConfig
  val log: Logger
  val isDryrun: Boolean

  val manager = SingularityManager(localConfig.environment.singularity, log, isDryrun = isDryrun)

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
        CommandError(s"Unable to connect to ${localConfig.environment.singularity.url}")
      case Success(_) =>
        processCmd()
    }

  }

  private def showCommand() = {
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

  def getRemoteRequest(localRequest: SingularityRequest): Try[Option[SingularityRequest]] = {
    log.debug("Fetching the remote request configuration...")
    manager.getSingularityRequest(localRequest.id).map(_.map(_.request))
  }

  def getActiveDeploy(localRequest: SingularityRequest): Try[Option[SingularityActiveDeployResponse]] = {
    log.debug("Fetching the remote active deploy...")
    manager.getSingularityRequest(localRequest.id).map(_.flatMap(_.activeDeploy))
  }

  /**
   * Create or update the Singularity request based on the diff between local and remote.
   */
  def updateSingularityRequestIfNeeded(remoteOpt: Option[SingularityRequest], local: SingularityRequest) = {

    log.debug("Comparing remote and local configuration...")

    remoteOpt match {
      case None =>
        log.info(s" No Mesos service found with id: '${local.id}'")
        manager.createSingularityRequest(local).map(_ => local)

      case Some(remote) if (remote.instances != local.instances) =>
        // Remote Singularity request exist but need to be updated.
        manager.scaleSingularityRequest(remote, local).map(_ => local)

      case Some(other) =>
        // Remote Singularity Request is up to date, nothing to do here!
        Success(log.info(s" The request configuration for '${localConfig.serviceName}' is up to date! [requestId: ${other.id}]"))
        Success(local)
    }
  }

}
