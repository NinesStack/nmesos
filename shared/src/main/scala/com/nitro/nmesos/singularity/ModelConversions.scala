package com.nitro.nmesos.singularity

import com.nitro.nmesos.config.model.{ CmdConfig, Environment }
import com.nitro.nmesos.singularity.model._
import com.nitro.nmesos.util.SequenceUtil
import org.joda.time.DateTime

/**
 * Conversion from local config models to Singularity Models.
 */
object ModelConversions {

  def toSingularityResources(environment: Environment) = SingularityResources(
    cpus = environment.resources.cpus,
    memoryMb = environment.resources.memoryMb,
    numPorts = environment.container.ports.map(_.size).getOrElse(0),
    diskMb = 0
  )

  def imageWithTag(config: CmdConfig): String = {
    val tag = if (config.tag.isEmpty) "latest" else config.tag
    s"${config.environment.container.image}:${tag}"
  }

  def toSingularityContainerInfo(config: CmdConfig): SingularityContainerInfo = {
    val network = config.environment.container.network.getOrElse("BRIDGE")
    val containerPorts = config.environment.container.ports.getOrElse(Seq.empty)

    if (network != "BRIDGE" && !containerPorts.isEmpty) {
      sys.error("Port mappings are only supported for bridge network")
    }

    val portMappings = containerPorts.zipWithIndex.map {
      case (port, index) =>
        SingularityDockerPortMapping(
          containerPort = port,
          hostPortType = "FROM_OFFER",
          containerPortType = "LITERAL",
          hostPort = index, // When using Literal Host (PORT0, PORT1, PORT2)
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
        forcePullImage = config.environment.container.forcePullImage.getOrElse(false),
        dockerParameters = dockerParameters
      )
    )
  }

  /**
   * Singularity Id can contain only "a-zA-Z0-9_"
   */
  def normalizeId(id: String): String = {
    id.replaceAll("[^a-zA-Z0-9_]", "_")
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
    val sequence = SequenceUtil.sequenceId()
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
    customExecutorCmd = config.environment.executor.flatMap(_.customExecutorCmd),
    env = config.environment.executor.flatMap(_.env_vars).getOrElse(Map.empty),
    autoAdvanceDeploySteps = config.environment.singularity.autoAdvanceDeploySteps
  )

  def toSingularityRequest(config: CmdConfig) = SingularityRequest(
    id = toSingularityRequestId(config),
    requestType = "SERVICE",
    instances = config.environment.resources.instances,
    slavePlacement = config.environment.singularity.slavePlacement.getOrElse("OPTIMISTIC"),
    requiredRole = config.environment.singularity.requiredRole
  )

  def describeDeploy(request: SingularityRequest, deploy: SingularityDeploy): Seq[String] = Seq(
    s"requestId: ${request.id}",
    s"deployId:  ${deploy.id}",
    s"image:     ${deploy.containerInfo.docker.image}",
    s"instances: ${request.instances}, slavePlacement: ${request.slavePlacement}",
    s"""resources: [cpus: ${deploy.resources.cpus}, memory: ${deploy.resources.memoryMb}Mb, role: ${request.requiredRole.getOrElse("*")}]""",
    s"""ports:     ${deploy.containerInfo.docker.portMappings.map(_.containerPort).mkString(",")}"""
  )

}
