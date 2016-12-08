package com.nitro.nmesos.cli

object Main {
  def main(args: Array[String]): Unit = {
    println(s"Nitro Mesos Deploy tool")
    CliManager.process(args)
  }
}

object CliManager {

  import java.io.File
  import com.nitro.nmesos.cli.model._
  import com.nitro.nmesos.commands.{ CommandError, CommandSuccess }
  import com.nitro.nmesos.util.{ InfoLogger, Logger, VerboseLogger }
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
    val log = if (cmd.verbose) VerboseLogger else InfoLogger

    val yamlFile = toFile(cmd)

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
  def executeCommand(cmd: Cmd, config: ValidConfig, log: Logger): Unit = cmd.action match {
    case ReleaseAction =>
      val serviceConfig = toServiceConfig(cmd, config)
      val cmdResult = ReleaseCommand(serviceConfig, log, isDryrun = cmd.isDryrun).run()

      cmdResult match {
        case CommandSuccess =>
          val warningDryRun = if (cmd.isDryrun) log.importantColor("[dryrun true]") else ""
          log.info(s"Done, without errors! $warningDryRun")

        case CommandError(error) =>
          log.error(error)
          sys.exit(1)

      }
    case other =>
      // nothing to do.
      log.error(s"Action '$other' not implemented yet :( ")
      sys.exit(1)

  }

  def showConfigError(cmd: Cmd, configError: ConfigError, log: Logger): Unit = {
    log.logBlock("Invalid config") {
      log.info(
        s""" Service Name: ${cmd.serviceName}
           | Config File: ${configError.yamlFile.getAbsolutePath}
             """.stripMargin
      )
      log.error(configError.msg)
    }
  }

  def toFile(cmd: Cmd): File = new File(s"${cmd.serviceName}.yml")

  // Cli command config to shared config model
  def toServiceConfig(cmd: Cmd, config: ValidConfig) = CmdConfig(
    serviceName = cmd.serviceName,
    force = cmd.force,
    tag = cmd.tag,
    environment = config.environment,
    environmentName = config.environmentName,
    fileHash = config.fileHash,
    file = config.file
  )
}
