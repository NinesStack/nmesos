package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.util.Logger
import com.nitro.nmesos.config.{Fail, Validations, Warning}
import com.nitro.nmesos.singularity.ModelConversions

/**
  * Create <service-name>.env file (to run a docker container locally).
  */
case class DockerEnvCommand(
    localConfig: CmdConfig,
    log: Logger,
    isDryrun: Boolean    
) extends BaseCommand {

  override def run(): CommandResult = {
    showCommand()
    processCmd()
  }

  override protected def processCmd(): CommandResult = {
    import java.io._

    log.logBlock("Creating ...") {
      val containerInfo = ModelConversions.toSingularityContainerInfo(localConfig)
      val imageWithTag = containerInfo.docker.image
      val portMappings = containerInfo.docker.portMappings
      val portString = if (portMappings.isEmpty) "[]" else "\n" ++ portMappings
        .map(pm => s"      - ${pm.hostPort}:${pm.containerPort}")
        .mkString("\n")
      val volumeMappings = containerInfo.volumes
      val volumeString = if (volumeMappings.isEmpty) "[]" else "\n" ++ volumeMappings
        .map(vm => s"      - ${vm.hostPath}:${vm.containerPath}")
        .mkString("\n")

      val composeFilename = s"docker-compose.${localConfig.serviceName}.yml"
      val composeString = s"""version: "3.8"
        >services:
        >
        >  ${localConfig.serviceName}:
        >    image: ${imageWithTag}
        >    ports: ${portString}
        >    volumes: ${volumeString}
        >    env_file: ${localConfig.serviceName}.env
      """.stripMargin('>')

      val composeFile = new PrintWriter(new File(composeFilename))
      composeFile.write(composeString)
      composeFile.close

      val envVarFilename = s"${localConfig.serviceName}.env"
      val envVarString = localConfig.environment.container.env_vars.get.toList
        .sortBy(_._1)
        .map { case (key, value) => s"${key}=${value}" }
        .mkString("\n")

      val envFile = new PrintWriter(new File(envVarFilename))
      envFile.write(envVarString)
      envFile.close

      CommandSuccess(s"Start container with: docker-compose [--detach] --file ${composeFilename} up")
    }
  }
}
