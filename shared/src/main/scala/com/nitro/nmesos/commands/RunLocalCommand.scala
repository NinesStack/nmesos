package com.nitro.nmesos.commands

import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.util.Logger
import com.nitro.nmesos.config.{Fail, Validations, Warning}
import sys.process._
import scala.language.postfixOps
import com.nitro.nmesos.singularity.ModelConversions

/**
  * Run the service locally
  */
case class RunLocalCommand(
    localConfig: CmdConfig,
    log: Logger,
    isDryrun: Boolean,
    deprecatedSoftGracePeriod: Int,
    deprecatedHardGracePeriod: Int
) extends BaseCommand {

  override def run(): CommandResult = {
    showCommand()
    processCmd()
  }

  def runLocalCmdString(): String = {
    val envVarString = localConfig.environment.container.env_vars.get
      .map { case (key, value) => s"""-e ${key}=${value}""" }
      .mkString(" ")

    val containerInfo = ModelConversions.toSingularityContainerInfo(localConfig)
    val imageWithTag = containerInfo.docker.image
    val portMappings = containerInfo.docker.portMappings
      .map(pm => s"-p ${pm.containerPort}:${pm.containerPort}")
      .mkString(" ")

    s"docker run -i ${portMappings} ${envVarString} ${imageWithTag}"
  }

  private def showCommand() = {
    log.logBlock("Local deploy for config") {
      log.info(
        s""" Service Name: ${localConfig.serviceName}
           | Config File:  ${localConfig.file.getAbsolutePath}
           | environment:  ${localConfig.environmentName}
           | image:        ${localConfig.environment.container.image}:${localConfig.tag}
           | docker cmd:   ${runLocalCmdString()}
           """
      )
    }
  }

  override def processCmd(): CommandResult = {
    val runLocal = (runLocalCmdString() !)
    if (runLocal == 0) {
      CommandSuccess(s"""Successfully deployed locally""")
    } else {
      CommandError(s"Unable to deploy locally")
    }
  }

}
