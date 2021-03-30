package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.ModelConversions.{
  DeployId,
  toSingularityRequest
}
import com.nitro.nmesos.singularity.model.{
  SingularityRequest,
  SingularityResources
}
import com.nitro.nmesos.util.Formatter
import com.nitro.nmesos.singularity.ModelConversions._
import com.nitro.nmesos.singularity.model.SingularityRequestParent.SingularityActiveDeployResponse

import scala.util.{Failure, Success, Try}

/**
  * Command to scale up/down a service in Mesos.
  * Will change resources (cpu, memory, instances) but keeping the some version already deployed.
  */
case class ScaleCommand(localConfig: CmdConfig, fmt: Formatter, isDryrun: Boolean)
    extends ScaleCommandHelper {

  def processCmd(): CommandResult = {

    val localRequest = toSingularityRequest(localConfig)

    ///////////////////////////////////////////////////////
    // Applying Configuration and Deploy if needed
    // - Check previous request
    // - create Request if needed
    // - check previous deploy
    // - deploy if needed
    val tryScale: Try[String] = fmt.fmtBlock("Applying Scale Config!") {
      for {
        remoteRequest <- getRemoteRequest(localRequest)
        updatedRequest <-
          scaleSingularityRequestIfNeeded(remoteRequest, localRequest)
        _ <- scaleDeployIfNeeded(localRequest)
      } yield updatedRequest.id
    }

    tryScale match {
      case Success(_) =>
        CommandSuccess(
          s"""Successfully scaled to ${localRequest.instances.getOrElse(
            "0"
          )} instances.$dryWarning"""
        )
      case Failure(ex) =>
        CommandError(s"Unable to deploy - ${ex.getMessage}")
    }
  }
}

trait ScaleCommandHelper extends BaseCommand {

  /**
    * Compare remote deploy running and desired deploy,
    * deploying a new Singularity Deploy if needed (to update cpus/ or memory).
    */
  def scaleDeployIfNeeded(localRequest: SingularityRequest): Try[Unit] = {

    fmt.debug(s"Checking if there is already an active deploy...")

    getActiveDeploy(localRequest).flatMap {
      case None =>
        fmt.info(s"There is no active deploy for request: '${localRequest.id}'")
        Success(()) // no deploy to scale.

      case Some(
            SingularityActiveDeployResponse(remoteDeployId, remoteResources)
          ) =>
        val localResource = toSingularityResources(localConfig.environment)

        // Already a deploy with same id, force required.
        if (
          remoteResources.memoryMb != localResource.memoryMb ||
          remoteResources.cpus != localResource.cpus ||
          remoteResources.numPorts != localResource.numPorts ||
          remoteResources.diskMb != localResource.diskMb
        ) {
          fmt.info(
            s" Current deploy '$remoteDeployId' is not using the required resources"
          )
          fmt.info(
            s""" Changes to apply:
               |   * memoryMb:  ${remoteResources.memoryMb}   -> ${localResource.memoryMb}
               |   * cpus:      ${remoteResources.cpus}     -> ${localResource.cpus}""".stripMargin
          )

          val localDeploy =
            toSingularityDeploy(localConfig, scaleDeployId(remoteDeployId))
          val message =
            s"Auto scaling deploy '$remoteDeployId' [memoryMb: ${remoteResources.memoryMb}->${localResource.memoryMb}, cpus:${remoteResources.cpus}->${localResource.cpus}]"
          manager
            .deploySingularityDeploy(localRequest, localDeploy, message)
            .map(_ => {})
        } else {
          fmt.info(s" No need to update the active deploy '$remoteDeployId'")
          fmt.info(s""" * memoryMb:  ${localResource.memoryMb}
               | * cpus:      ${localResource.cpus}
               | """.stripMargin)
          Success(())
        }

    }

  }

  def scaleDeployId(currentDeployId: DeployId) = {
    val id = currentDeployId.takeWhile(_ != '_') // clean up previous deployId
    generateRandomDeployId(id)
  }
}
