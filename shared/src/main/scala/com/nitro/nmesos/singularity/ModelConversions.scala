package com.nitro.nmesos.singularity

import com.nitro.nmesos.config.model.{ CmdConfig, Environment }
import com.nitro.nmesos.singularity.model._

/**
 * Conversion from local config models to Singularity Models.
 */
object ModelConversions {

  def toSingularityResources(environment: Environment) = SingularityResources(
    cpus = environment.resources.cpus,
    memoryMb = environment.resources.memoryMb,
    numPorts = environment.container.ports.size,
    diskMb = 0
  )

  def imageWithTag(config: CmdConfig): String = {
    s"${config.environment.container.image}:${config.tag}"
  }

  def toSingularityContainerInfo(config: CmdConfig): SingularityContainerInfo = {
    val network = config.environment.container.network.getOrElse("BRIDGE")
    val containerPorts = config.environment.container.ports.getOrElse(Seq.empty)

    if (network != "BRIDGE" && !containerPorts.isEmpty) {
      sys.error("Port mappings are only supported for bridge network")
    }

    val portMappings = containerPorts.map { port =>
      SingularityDockerPortMapping(
        containerPort = port,
        hostPortType = "FROM_OFFER",
        containerPortType = "LITERAL",
        hostPort = 0, // It has to be 0!
        protocol = "tcp"
      )
    }

    val labels = config.environment.container.labels
      .getOrElse(Map.empty).map { case (key, value) => SingularityDockerParameter("label", s"$key=$value") }.toSeq

    val envVars = config.environment.container.env_vars
      .getOrElse(Map.empty).map { case (key, value) => SingularityDockerParameter("env", s"$key=$value") }.toSeq

    val extraDockerParameters = config.environment.container.dockerParameters.getOrElse(Map.empty)
      .map { case (key, value) => SingularityDockerParameter(key, value) }.toSeq

    val dockerParameters = labels ++ envVars ++ extraDockerParameters

    val volumes = config.environment.container.volumes.getOrElse(Seq.empty).map { volumenMapping =>
      volumenMapping.split(":").toSeq match {
        case Seq(hostPath, containerPath) => SingularityVolume(hostPath, containerPath, "RW")
        case Seq(hostPath, containerPath, mode) => SingularityVolume(hostPath, containerPath, mode)
      }
    }

    SingularityContainerInfo(
      `type` = "DOCKER",
      volumes = volumes,
      docker = SingularityDockerInfo(
        network = network,
        image = imageWithTag(config),
        portMappings = portMappings,
        dockerParameters = dockerParameters
      )
    )
  }

  /**
   * Singularity Id can not contain any of the following characters @, -, \, /, *, ?, %,  , [, ], #, $
   */
  def normalizeId(id: String): String = {
    id.replaceAll("[@ \\- \\\\  \\/ * ? % \\[ \\] #$ ]", "_")
  }

  type RequestId = String

  def toSingularityRequestId(config: CmdConfig): RequestId = {
    normalizeId(s"${config.environmentName}-${config.serviceName}")
  }

  type DeployId = String

  /**
   * Deploy Id used if there are no collisions.
   */
  def defaultDeployId(config: CmdConfig): DeployId = {
    normalizeId(s"${config.tag}-${config.fileHash}")
  }

  /**
   * Deploy Id used if there are collisions
   */
  def generateRandomDeployId(defaultId: DeployId): DeployId = {
    val sequence = System.currentTimeMillis
    normalizeId(s"${defaultId}-$sequence")
  }

  def toSingularityDeploy(config: CmdConfig, deployId: DeployId) = SingularityDeploy(
    requestId = toSingularityRequestId(config),
    id = deployId,
    resources = toSingularityResources(config.environment),
    containerInfo = toSingularityContainerInfo(config),
    healthcheckUri = config.environment.singularity.healthcheckUri,
    deployInstanceCountPerStep = config.environment.singularity.deployInstanceCountPerStep,
    deployStepWaitTimeMs = config.environment.singularity.deployStepWaitTimeMs.getOrElse(0),
    autoAdvanceDeploySteps = config.environment.singularity.autoAdvanceDeploySteps
  )

  def toSingularityRequest(config: CmdConfig) = SingularityRequest(
    id = toSingularityRequestId(config),
    requestType = "SERVICE",
    instances = config.environment.resources.instances,
    bounceAfterScale = false,
    slavePlacement = config.environment.singularity.slavePlacement.getOrElse("OPTIMISTIC")
  )

  def describeDeploy(request: SingularityRequest, deploy: SingularityDeploy): Seq[String] = Seq(
    s"requestId: ${request.id}",
    s"deployId:  ${deploy.id}",
    s"image:     ${deploy.containerInfo.docker.image}",
    s"instances: ${request.instances}",
    s"resources: [cpus: ${deploy.resources.cpus}, memory: ${deploy.resources.memoryMb}Mb]",
    s"""ports:   ${deploy.containerInfo.docker.portMappings.map(_.containerPort).mkString(",")}"""
  )

}
