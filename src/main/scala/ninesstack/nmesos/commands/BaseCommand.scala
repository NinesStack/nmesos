package ninesstack.nmesos.commands

import ninesstack.nmesos.config.model.CmdConfig
import ninesstack.nmesos.singularity.SingularityManager
import ninesstack.nmesos.singularity.model.SingularityRequestParent.SingularityActiveDeployResponse
import ninesstack.nmesos.singularity.model.{
  SingularityRequest,
  SingularityResources,
  SingularityUpdateResult
}
import ninesstack.nmesos.util.Formatter

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
  private val logger = org.log4s.getLogger
  
  val localConfig: CmdConfig
  val fmt: Formatter
  val isDryrun: Boolean

  val manager = SingularityManager(
    localConfig.environment.singularity,
    fmt,
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
    fmt.fmtBlock("Deploying Config") {
      fmt.info(
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
    logger.info("Fetching the remote request configuration...")
    manager.getSingularityRequest(localRequest.id).map(_.map(_.request))
  }

  def getActiveDeploy(
      localRequest: SingularityRequest
  ): Try[Option[SingularityActiveDeployResponse]] = {
    logger.info("Fetching the remote active deploy...")
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
    logger.info("Comparing remote and local configuration...")
    remoteOpt match {
      case None =>
        fmt.info(s" No Mesos config found with id: '${local.id}'")
        manager.createSingularityRequest(local).map(_ => local)

      case Some(remote) if (remote != local) =>
        // Remote Singularity request exist but need to be updated.
        manager.updateSingularityRequest(remote, local).map(_ => local)

      case Some(other) =>
        // Remote Singularity Request is up to date, nothing to do here!
        Success(
          fmt.info(
            s" The request configuration for '${localConfig.serviceName}' is up to date! [requestId: ${other.id}]"
          )
        )
        Success(local)
    }
  }

  def dryWarning =
    if (isDryrun) fmt.importantColor(" [dry-run true] use --dry-run false")
    else ""

}
