package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.util.Logger
import com.nitro.nmesos.config.{Fail, Validations, Warning}

import sys.process._
import scala.language.postfixOps

/**
  * Create <service-name>.env and a docker-compose.<service-name>.yml
  *  file and run docker-compose up to start the service (in the background).
  */
case class DockerRunCommand(
    localConfig: CmdConfig,
    log: Logger,
    isDryrun: Boolean    
) extends BaseCommand {

  override def run(): CommandResult = {
    showCommand()
    processCmd()
  }

  override protected def processCmd(): CommandResult = {
    log.logBlock("Starting service ...") {
      val (_, composeFilename) = DockerEnvCommand.createDockerFiles(localConfig)

      val dockerCommand = s"docker-compose --file ${composeFilename} up --detach"
      (dockerCommand !) match {
        case 0 => CommandSuccess(s"... ${localConfig.serviceName} started!\nStop it with: docker-compose --file ${composeFilename} down --remove-orphans")
        case _ => CommandError(s"Unable to start service!")
      }
    }
  }
}
