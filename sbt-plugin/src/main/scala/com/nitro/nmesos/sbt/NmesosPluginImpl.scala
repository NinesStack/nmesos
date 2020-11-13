package com.nitro.nmesos.sbt

import com.nitro.nmesos.commands.{CommandError, CommandSuccess, ReleaseCommand}
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.{ConfigError, ValidConfig}
import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.sbt.model.ReleaseArgs
import com.nitro.nmesos.util.{Logger => NLogger}
import sbt._
import sbt.Logger

/**
  * Parse sbt args, read the configuration file and execute the command.
  */
object NmesosPluginImpl {

  val ConfigRepositoryEnvName = "NMESOS_CONFIG_REPOSITORY"

  def resolveRepositoryPath(defaultPath: File, log: Logger) = {

    val envValue = sys.env.get(ConfigRepositoryEnvName)
    val path = envValue match {
      case None =>
        log.warn(
          s"[Nmesos] Environment var $ConfigRepositoryEnvName is not defined, using default path ${defaultPath.getAbsolutePath}"
        )
        defaultPath.getAbsolutePath
      case Some(path) =>
        log.info(s"[Nmesos] Environment var $ConfigRepositoryEnvName=$path")
        path
    }

    log.info(s"[Nmesos] Configuration repo: $path")

    path
  }

  def release(
      args: ReleaseArgs,
      serviceName: String,
      repositoryConfigPath: File,
      localVersion: String,
      logger: Logger
  ) = {

    val yamlFile = repositoryConfigPath / s"$serviceName.yml"

    if (!yamlFile.exists()) {
      logger.error(s"[Nmesos] Config file $yamlFile missing!")
      sys.error(s"Config file $yamlFile doesn't exist.")
    } else {
      val log = SbtCustomLogger(logger)
      log.logBlock("Sbt project info") {
        logger.info(
          s""" Configuration Repo:          $repositoryConfigPath
             | Configuration service file:  $yamlFile
             | Local version:               ${log.infoColor(localVersion)}
             | Version to deploy:           ${log.infoColor(
            args.tag
          )}""".stripMargin
        )
      }

      ConfigReader.parseEnvironment(yamlFile, args.environment, log) match {
        case error: ConfigError =>
          showConfigError(serviceName, error, log)

        case config: ValidConfig =>
          executeCommand(args, serviceName, config, log)
      }
    }

  }

  def executeCommand(
      args: ReleaseArgs,
      serviceName: String,
      config: ValidConfig,
      log: NLogger
  ) = {
    val serviceConfig = toServiceConfig(serviceName, args, config)
    val cmdResult =
      ReleaseCommand(serviceConfig, log, isDryrun = args.isDryrun).run()

    cmdResult match {
      case CommandSuccess(msg) =>
        log.info(msg)

      case CommandError(error) =>
        log.error(error)
    }

  }

  def showConfigError(
      serviceName: String,
      configError: ConfigError,
      log: NLogger
  ): Unit = {
    log.logBlock("Invalid config") {
      log.info(s""" Service Name: $serviceName
           | Config File: ${configError.yamlFile.getAbsolutePath}
             """.stripMargin)
      log.error(configError.msg)
    }
    sys.error(configError.msg)
  }

  def toServiceConfig(
      serviceName: String,
      cmd: ReleaseArgs,
      config: ValidConfig
  ) =
    CmdConfig(
      serviceName = serviceName,
      force = cmd.force,
      tag = cmd.tag,
      environment = config.environment,
      environmentName = config.environmentName,
      fileHash = config.fileHash,
      file = config.file
    )

}
