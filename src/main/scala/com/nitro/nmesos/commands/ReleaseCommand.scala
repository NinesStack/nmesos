package com.nitro.nmesos.commands

import com.nitro.nmesos.config.{Fail, Validations}
import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.model._

import scala.util.{Failure, Success, Try}
import com.nitro.nmesos.singularity.ModelConversions._
import com.nitro.nmesos.util.{Logger, WaitUtil}
import com.nitro.nmesos.util.Conversions._

import scala.annotation.tailrec

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
case class ReleaseCommand(
    localConfig: CmdConfig,
    log: Logger,
    isDryrun: Boolean,
    deprecatedSoftGracePeriod: Int,
    deprecatedHardGracePeriod: Int
) extends DeployCommandHelper {

  def verifyCommand(): Try[Unit] =
    log.logBlock("Verifying") {
      val checks = Validations.checkAll(
        localConfig,
        (deprecatedSoftGracePeriod, deprecatedHardGracePeriod)
      )
      val errors = checks.collect { case f: Fail => f }
      Validations.logResult(checks, log)
      if (errors.isEmpty) Success(())
      else Failure(new Exception(s"Invalid Config"))
    }

  def processCmd(): CommandResult = {

    val localRequest = toSingularityRequest(localConfig)

    val isValid = verifyCommand()

    ///////////////////////////////////////////////////////
    // Applying Configuration and Deploy if needed
    // - Check previous request
    // - create Request if needed
    // - check previous deploy
    // - deploy if needed
    val tryDeployId: Try[DeployId] = log.logBlock("Applying config!") {
      for {
        _ <- isValid
        remoteRequest <- getRemoteRequest(localRequest)
        updatedRequest <-
          updateSingularityRequestIfNeeded(remoteRequest, localRequest)

        deployedId <- deployVersionIfNeeded(updatedRequest)
      } yield deployedId
    }

    ///////////////////////////////////////////////////////
    // Show Mesos task info
    val tryShowDeployStatus = for {
      deployId <- tryDeployId
      isSuccess <-
        if (!isDryrun) showFinalDeployStatus(localRequest, deployId)
        else Success(true)
      _ <-
        if (!isDryrun && !isSuccess) showLogs(localRequest, deployId)
        else Success(true)
    } yield {
      isSuccess
    }

    tryShowDeployStatus match {
      case Success(true) if (localRequest.schedule.isEmpty) =>
        CommandSuccess(
          s"""Successfully deployed to ${localRequest.instances.getOrElse(
            ""
          )} instances.$dryWarning"""
        )
      case Success(true) if (localRequest.schedule.isDefined) =>
        CommandSuccess(
          s"""Successfully scheduled - cron: '${localRequest.schedule.getOrElse(
            ""
          )}'.$dryWarning"""
        )
      case Success(false) =>
        CommandError(s"Unable to deploy")
      case Failure(ex) =>
        CommandError(s"Unable to deploy - ${ex.getMessage}")
      case _ =>
        throw new RuntimeException("Unexpected case")
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
        val newImage = localDeploy.containerInfo.docker.image
        val message = s" Deploying version '$newImage'"
        manager
          .deploySingularityDeploy(local, localDeploy, message)
          .map(_ => localDeploy.id)

      case Some(_) =>
        // Already a deploy with same id, force required.
        if (localConfig.force) {
          val deployId = generateRandomDeployId(defaultId)
          val localDeploy = toSingularityDeploy(localConfig, deployId)

          log.info(
            s" There is already a deploy with id $defaultId , forcing deploy with new id '$deployId'"
          )
          val newImage = localDeploy.containerInfo.docker.image
          val message = s" Redeploy of $deployId forced. Image: $newImage"

          manager
            .deploySingularityDeploy(local, localDeploy, message)
            .map(_ => localDeploy.id)

        } else {
          log.info(
            s" There is already a deploy with same id '$defaultId' for request '${local.id}'"
          )
          Failure(
            sys.error(
              s"There is already a deploy with id $defaultId, use --force to force the redeploy"
            )
          )
        }

    }

  }

  // Show the Mesos TaskId and final deploy status.
  final def showFinalDeployStatus(
      request: SingularityRequest,
      deployId: DeployId
  ): Try[Boolean] = {
    log.logBlock("Mesos Tasks Info") {
      log.info(
        s" Deploy progress at ${localConfig.environment.singularity.url}/request/${request.id}/deploy/$deployId"
      )

      val managerWithoutLogger = manager.withDisabledDebugLog()

      ///////////////////////////////////////////////////////
      // Wait until the pending task are executed.
      def fetchMessage() = {
        for {
          pending <-
            managerWithoutLogger
              .getSingularityPendingDeploy(request.id, deployId)
              .getOrElse(None)
        } yield {
          val count = pending.deployProgress.targetActiveInstances
          val status = pending.currentDeployState
          s"""Waiting until the deploy is completed [deployId: '$deployId', status: $status, instances ${count}/${request.instances
            .getOrElse("*")}]"""
        }
      }

      log.showAnimated(fetchMessage)

      // hack, need to wait until Singularity move the deploy result to History.
      WaitUtil.waitUntil {
        log.debug(s"Waiting for the deploy result...")
        for {
          deployInfo <-
            manager.getSingularityDeployHistory(request.id, deployId)
        } yield {
          deployInfo.flatMap(_.deployResult).isDefined
        }
      }

      ///////////////////////////////////////////////////////
      // Fetch Active task and inactive(history) task in Singularity for this request.
      // Show relevant information
      for {
        deployInfo <- manager.getSingularityDeployHistory(request.id, deployId)
        activeTasks <- managerWithoutLogger.getActiveTasks(request)
      } yield {
        val deploy =
          deployInfo.getOrElse(sys.error(s"Unable to find deployId $deployId"))
        val deployResult =
          deploy.deployResult.getOrElse(sys.error(s"Missing deploy result."))

        localConfig.environment.singularity.schedule match {
          case None =>
            // Service task
            val failureTasksId = deployResult.deployFailures.map(_.taskId.id)
            val successfulTasks = activeTasks
              .filter(_.taskId.deployId == deployId)
              .filterNot(task => failureTasksId.contains(task.taskId.id))
            logTaskInfo(deployResult, successfulTasks)
          case Some(schedule) =>
            // job task
            logJobInfo(request, deployResult, schedule)
        }

        deployResult.deployState.equalsIgnoreCase("SUCCEEDED")
      }
    }
  }

  private def logJobInfo(
      request: SingularityRequest,
      deployResult: SingularityDeployResult,
      schedule: String
  ) = {
    log.println(s""" Scheduled Mesos Job State: ${log.importantColor(
      deployResult.deployState
    )}""")
    log.println(
      s"   * History: ${localConfig.environment.singularity.url}/request/${request.id}"
    )
    log.println(s"   * Cron:    '$schedule'")
  }

  private def logTaskInfo(
      deployResult: SingularityDeployResult,
      successfulTasks: Seq[SingularityTask]
  ) = {
    val message = deployResult.message.map(msg => s" - $msg").getOrElse("")
    log.println(s""" Deploy Mesos Deploy State: ${log.importantColor(
      deployResult.deployState
    )}$message""")

    deployResult.deployFailures.sortBy(_.taskId.instanceNo).foreach { failure =>
      log.println(s"   * TaskId: ${log.infoColor(failure.taskId.id)}")
      log.println(s"      - Reason:  ${log.importantColor(failure.reason)}")
      failure.message.foreach(msg => log.println(s"      - Message: $msg"))
      log.println(s"      - Host:    ${failure.taskId.host}")
    }

    successfulTasks.foreach { task =>
      log.println(s"""   * TaskId: ${log.infoColor(task.taskId.id)}""")
      task.mesosTask.container.docker.portMappings.foreach { port =>
        log.println(
          s"""     - ${task.offer.hostname}:${port.hostPort}  -> ${port.containerPort}"""
        )
      }
    }
  }

  // fetch and print logs for all the task in a given deployId
  def showLogs(request: SingularityRequest, deployId: DeployId): Try[Unit] = {
    for {
      activeTask <- manager.getActiveTasks(request)
      tasks <- manager.getSingularityTaskHistory(request.id, deployId)
    } yield {
      val taskOption = tasks.map(_.taskId) ++ activeTask
        .map(_.taskId)
        .headOption // show logs only for the first task
      taskOption.foreach(taskId => showLog(taskId))
    }
  }.recover {
    case ex => // Ignore error while fetching logs.
      log.debug(s"Unable to fetch logs -  ${ex.getMessage}")
      ()
  }

  // Fetch and print logs for the given task
  def showLog(taskId: SingularityTaskId) = {
    val logsStdOut =
      manager.getLogs(taskId = taskId.id, path = "stdout").getOrElse(Seq.empty)
    val logsStdErr =
      manager.getLogs(taskId = taskId.id, path = "stderr").getOrElse(Seq.empty)

    log.logBlock("Log - stderr") {
      logsStdErr.foreach(line => log.println(s" $line"))
    }
    log.logBlock("Log - stdout") {
      logsStdOut.foreach(line => log.println(s" $line"))
    }
  }
}
