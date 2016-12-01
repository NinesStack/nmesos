package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.SingularityManager
import com.nitro.nmesos.singularity.model._
import scala.util.{ Failure, Success, Try }
import com.nitro.nmesos.singularity.ModelConversions._
import com.nitro.nmesos.util.Logger

sealed trait CommandResult
case object CommandSuccess extends CommandResult
case class CommandError(msg: String) extends CommandResult

/**
 * Command to deploy a service to Mesos.
 * Features:
 *  - Create or update the Singularity Request if needed.
 *  - Scale up/down the Singularity Request/Deploy if needed.
 *  - Check if the deploy already exist. (require force action for redeploy)
 *  - Run in dryrun mode or real mode.
 *  - deploy a new version of the service
 *  - show Mesos task status.
 */
case class ReleaseCommand(localConfig: CmdConfig, log: Logger, isDryrun: Boolean) extends DeployCommandHelper {

  def run(): CommandResult = {

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

    // Check visibility before attesting to process the command.
    manager.ping() match {
      case Failure(_) =>
        CommandError(s"Unable to connect to ${localConfig.environment.singularity.url}")
      case Success(_) =>
        processCmd()
    }

  }

  private def processCmd(): CommandResult = {

    val localRequest = toSingularityRequest(localConfig)

    ///////////////////////////////////////////////////////
    // Applying Configuration and Deploy if needed
    // - Check previous request
    // - create Request if needed
    // - check previous deploy
    // - deploy if needed
    val tryDeployId: Try[DeployId] = log.logBlock("Applying config!") {
      for {
        remoteRequest <- getRemoteRequest(localRequest)
        updatedRequest <- updateSingularityRequestIfNeeded(remoteRequest, localRequest)

        deployedId <- deployVersionIfNeeded(updatedRequest)
      } yield deployedId
    }

    ///////////////////////////////////////////////////////
    // Show Mesos task info if successful deploy
    tryDeployId.flatMap { deployId =>
      if (!isDryrun) showFinalDeployStatus(localRequest, deployId) else Success(())
    }

    tryDeployId match {
      case Success(_) =>
        CommandSuccess
      case Failure(ex) =>
        CommandError(s"Unable to deploy - ${ex.getMessage}")
    }
  }
}

trait DeployCommandHelper {
  self: ReleaseCommand =>

  val manager = SingularityManager(localConfig.environment.singularity, log, isDryrun = isDryrun)

  def getRemoteRequest(localRequest: SingularityRequest) = {
    log.debug("Fetching the remote request configuration...")
    manager.getSingularityRequest(localRequest.id)
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
        // TODO check cpu/mem changes too (for the deploy)
        manager.scaleSingularityRequest(remote, local).map(_ => local)

      case Some(other) =>
        // Remote Singularity Request is up to date, nothing to do here!
        Success(log.info(s" The configuration for '${localConfig.serviceName}' is up to date! [requestId: ${other.id}]"))
        Success(local)
    }
  }

  /**
   * Compare remote deploy running and desired deploy, deploying a new Singularity Deploy if needed.
   */
  def deployVersionIfNeeded(local: SingularityRequest): Try[DeployId] = {
    val defaultId = defaultDeployId(localConfig)

    log.debug(s"Checking if a deploy with id '$defaultId' already exist...")

    manager.getSingularityDeployHistory(local.id, defaultId).flatMap {
      case None =>
        log.debug(s"There is no deploy with id '$defaultId'")
        val localDeploy = toSingularityDeploy(localConfig, defaultId)
        manager.deploySingularityDeploy(local, localDeploy).map(_ => localDeploy.id)

      case Some(_) =>
        // Already a deploy with same id, force required.
        if (localConfig.force) {
          val deployId = generateRandomDeployId(defaultId)
          val localDeploy = toSingularityDeploy(localConfig, deployId)
          log.info(s" There is already a deploy with id $defaultId , forcing deploy with new id $deployId")
          manager.deploySingularityDeploy(local, localDeploy).map(_ => localDeploy.id)
        } else {
          log.info(s" There is already a deploy with same id '$defaultId' for request '${local.id}")
          Failure(sys.error(s"There is already a deploy with id $defaultId, use --force to force the redeploy"))
        }

    }

  }

  // Show the Mesos TaskId and final deploy status.
  final def showFinalDeployStatus(request: SingularityRequest, deployId: DeployId): Try[Unit] = {
    log.logBlock("Mesos Tasks Info") {
      log.info(s" Deploy progress at ${localConfig.environment.singularity.url}/request/${request.id}/deploy/$deployId")

      ///////////////////////////////////////////////////////
      // Wait until the pending task are executed.
      def fetchMessage() = {
        manager.withDisabledDebugLog()
          .getSingularityPendingDeploy(request.id, deployId)
          .getOrElse(None).map { info =>
            val count = info.deployProgress.targetActiveInstances
            val status = info.currentDeployState
            s"Waiting until the deploy is completed [deployId: '$deployId', status: $status, instances ${count}/${request.instances}]"
          }
      }
      log.showAnimated(fetchMessage)

      ///////////////////////////////////////////////////////
      // Fetch Active task and inactive(history) task in Singularity for this request.
      // Show relevant information
      for {
        deployInfo <- manager.getSingularityDeployHistory(request.id, deployId)
        activeTasks <- manager.getActiveTask(request)
      } yield {
        val deploy = deployInfo.getOrElse(sys.error(s"Unable to find deployId $deployId"))

        log.info(s" Deploy Mesos Deploy State: ${deploy.deployResult.deployState}")

        activeTasks
          .filter(_.taskId.deployId == deployId)
          .foreach { task =>
            log.println(s"""   * TaskId: ${log.infoColor(task.taskId.id)}""")
            task.mesosTask.container.docker.portMappings.foreach { port =>
              log.println(s"""     - http://${task.taskId.host}:${port.hostPort}  -> ${port.containerPort}""")
            }
          }
      }
    }
  }
}
