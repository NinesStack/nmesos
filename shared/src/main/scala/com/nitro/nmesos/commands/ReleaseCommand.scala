package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.SingularityManager
import com.nitro.nmesos.singularity.model._

import scala.util.{ Failure, Success, Try }
import com.nitro.nmesos.singularity.ModelConversions._
import com.nitro.nmesos.util.Logger

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

  def processCmd(): CommandResult = {

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
    val tryGetStatus = tryDeployId.flatMap { deployId =>
      if (!isDryrun) showFinalDeployStatus(localRequest, deployId) else Success(true)
    }

    tryGetStatus match {
      case Success(true) =>
        CommandSuccess
      case Success(false) =>
        CommandError(s"Unable to deploy")
      case Failure(ex) =>
        CommandError(s"Unable to deploy - ${ex.getMessage}")
    }
  }
}

trait DeployCommandHelper extends BaseCommand {

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
        val newImage = s"${localDeploy.containerInfo.docker.image}:${localConfig.tag}"
        val message = s" Deploying version '$newImage'"
        manager.deploySingularityDeploy(local, localDeploy, message).map(_ => localDeploy.id)

      case Some(_) =>
        // Already a deploy with same id, force required.
        if (localConfig.force) {
          val deployId = generateRandomDeployId(defaultId)
          val localDeploy = toSingularityDeploy(localConfig, deployId)

          log.info(s" There is already a deploy with id $defaultId , forcing deploy with new id '$deployId'")
          val newImage = s"${localDeploy.containerInfo.docker.image}:${localConfig.tag}"
          val message = s"Redeploy of $deployId forced. Image: $newImage"

          manager.deploySingularityDeploy(local, localDeploy, message).map(_ => localDeploy.id)

        } else {
          log.info(s" There is already a deploy with same id '$defaultId' for request '${local.id}'")
          Failure(sys.error(s"There is already a deploy with id $defaultId, use --force to force the redeploy"))
        }

    }

  }

  // Show the Mesos TaskId and final deploy status.
  final def showFinalDeployStatus(request: SingularityRequest, deployId: DeployId): Try[Boolean] = {
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

        val tasks = activeTasks
          .filter(_.taskId.deployId == deployId)

        tasks.foreach { task =>
          log.println(s"""   * TaskId: ${log.infoColor(task.taskId.id)}""")
          task.mesosTask.container.docker.portMappings.foreach { port =>
            log.println(s"""     - http://${task.offer.hostname}:${port.hostPort}  -> ${port.containerPort}""")
          }
        }
        // The operation is successful if number of active task is equal requested task
        tasks.size == request.instances
      }
    }
  }

}
