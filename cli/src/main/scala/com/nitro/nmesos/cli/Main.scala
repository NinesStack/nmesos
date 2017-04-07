package com.nitro.nmesos.cli

import com.nitro.nmesos.BuildInfo
import com.nitro.nmesos.commands.{ CheckCommand, CommandResult, ScaleCommand }

object Main {
  def main(args: Array[String]): Unit = {
    println("NMesos Deploy tool")
    println(s"version: ${BuildInfo.version}")
    CliManager.process(args)
  }
}

object CliManager {

  import java.io.File
  import com.nitro.nmesos.cli.model._
  import com.nitro.nmesos.commands.{ CommandError, CommandSuccess }
  import com.nitro.nmesos.util.{ Logger, CustomLogger }
  import com.nitro.nmesos.commands.ReleaseCommand
  import com.nitro.nmesos.config.ConfigReader
  import com.nitro.nmesos.config.ConfigReader.{ ConfigError, ValidConfig }
  import com.nitro.nmesos.config.model.CmdConfig

  def process(args: Array[String]) = {
    CliParser.parse(args) match {
      case None =>
      // Invalid args, nothing to do
      case Some(cmd) =>
        processCmd(cmd)
    }
  }

  /**
   * Process the CLI input command and verify configuration.
   */
  def processCmd(cmd: Cmd) = {
    val log = CustomLogger(verbose = cmd.verbose, ansiEnabled = cmd.isFormatted)

    val yamlFile = toFile(cmd, log)

    ConfigReader.parseEnvironment(yamlFile, cmd.environment, log) match {
      case error: ConfigError =>
        showConfigError(cmd, error, log)

      case config: ValidConfig =>
        executeCommand(cmd, config, log)
    }
  }

  /**
   * Execute the detected command for a valid configuration.
   */
  def executeCommand(cmd: Cmd, config: ValidConfig, log: Logger): Unit = {
    val cmdResult: CommandResult = cmd.action match {
      case ReleaseAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        ReleaseCommand(serviceConfig, log, isDryrun = cmd.isDryrun).run()
      case ScaleAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        ScaleCommand(serviceConfig, log, isDryrun = cmd.isDryrun).run()
      case CheckAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        CheckCommand(serviceConfig, log, isDryrun = cmd.isDryrun).run()
      case other =>
        // nothing to do.
        log.error(s"Action '$other' not implemented yet :( ")
        sys.exit(1)
    }

    cmdResult match {
      case CommandSuccess(msg) =>
        log.info(msg)

      case CommandError(error) =>
        log.error(error)
        exitWithError()
    }
  }

  def showConfigError(cmd: Cmd, configError: ConfigError, log: Logger): Unit = {
    log.logBlock("Invalid config") {
      log.info(
        s""" Service Name: ${sanitizeServiceName(cmd.serviceName)}
           | Config File: ${configError.yamlFile.getAbsolutePath}
             """.stripMargin
      )
      log.error(configError.msg)
    }
    exitWithError()
  }

  val ConfigRepositoryEnvName = "NMESOS_CONFIG_REPOSITORY"

  def toFile(cmd: Cmd, log: Logger): File = {
    sys.env.get(ConfigRepositoryEnvName) match {
      case None =>
        new File(s"${cmd.serviceName}.yml")
      case Some(path) =>
        log.println(s"$ConfigRepositoryEnvName: $path")
        new File(s"${path}${File.separator}${cmd.serviceName}.yml")
    }
  }

  def sanitizeServiceName(serviceName: String) = {
    serviceName.split(File.separatorChar).last
  }

  // Cli command config to shared config model
  def toServiceConfig(cmd: Cmd, config: ValidConfig) = CmdConfig(
    serviceName = sanitizeServiceName(cmd.serviceName),
    force = cmd.force,
    tag = cmd.tag,
    environment = config.environment,
    environmentName = config.environmentName,
    fileHash = config.fileHash,
    file = config.file
  )

  def exitWithError() = sys.exit(1)
}
