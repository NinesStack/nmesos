package com.nitro.nmesos.cli

object Main {
  def main(args: Array[String]): Unit = {
    CliManager.process(args)
  }
}

object CliManager {
  import com.nitro.nmesos.cli.model._
  import com.nitro.nmesos.commands._
  import com.nitro.nmesos.config._
  import com.nitro.nmesos.config.model._
  import com.nitro.nmesos.config.ConfigReader._
  import com.nitro.nmesos.util._

  import java.io.File

  private val logger = org.log4s.getLogger

  def process(args: Array[String]) = {
    logger.info(s"Commandline args: ${args.toList}")

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
    logger.info(s"Commandline: ${cmd}")

    val fmt = CustomFormatter(ansiEnabled = cmd.isFormatted)

    cmd.action match {
      case VersionAction =>
        val result = VersionCommand().run()
        exit(fmt, result)
      case VerifyAction =>
        val result = VerifyEnvCommand(cmd.singularity, fmt).run()
        exit(fmt, result)
      case _ =>
        processYmlCommand(cmd, fmt)
    }
  }

  /**
    * Process all ymls in a deploy chain.
    * Log and exit on the first error
    */
  def processYmlCommand(initialCmd: Cmd, fmt: Formatter) = {
    val commandChain = getCommandChain(initialCmd, fmt)

    commandChain match {
      case Left(error) =>
        fmt.error(error)
        exitWithError()

      case Right(chain) =>
        val onSuccess = chain._1
        val onFailure = chain._2

        for ((cmd, config) <- onSuccess) {
          if (config.environment.container.deploy_freeze.getOrElse(false)) {
            fmt.error(
              "Attention: deploy_freeze set to true. You're not able to deploy this config."
            )
            exitWithError()
          } else {
            executeCommand(cmd, config, fmt) match {
              case CommandSuccess(msg) =>
                fmt.info(msg)

              case CommandError(error) =>
                fmt.error(error)
                maybeExecuteFailureCommandAndExit(onFailure, fmt)
            }
          }
        }
    }
  }

  type CommandAndConfig = (Cmd, ValidConfig)

  type CommandChainOnSuccess = Either[ConfigError, List[CommandAndConfig]]

  type CommandOnFailure = Either[ConfigError, Option[CommandAndConfig]]

  type CommandChain =
    Either[ConfigError, (List[CommandAndConfig], Option[CommandAndConfig])]

  /**
    * Parses and returns a valid chain of Commands and their corresponding Config file.
    * Returns a Left[ConfigError] on the first invalid command/config
    */
  def getCommandChain(initialCmd: Cmd, fmt: Formatter): CommandChain = {
    getConfigFromCmd(initialCmd, fmt) match {
      case e: ConfigError => Left(e)

      case initialConfig: ValidConfig =>
        val cmdChainOnSuccess = buildCmdChainOnSuccess(
          initialCmd,
          initialConfig,
          Right(List()),
          List(),
          initialCmd,
          fmt
        )
        val cmdOnFailure = buildCmdOnFailure(initialConfig, initialCmd, fmt)

        (cmdChainOnSuccess, cmdOnFailure) match {
          case (Left(e), _) => Left(e)
          case (_, Left(e)) => Left(e)

          case (Right(cmdChainOnSuccess), Right(cmdOnFailure)) =>
            Right((cmdChainOnSuccess, cmdOnFailure))
        }
    }
  }

  private def buildCmdChainOnSuccess(
      cmd: Cmd,
      configForCmd: ValidConfig,
      chain: CommandChainOnSuccess,
      cmdQueue: List[Cmd],
      initialCmd: Cmd,
      fmt: Formatter
  ): CommandChainOnSuccess =
    chain match {
      case Left(error) => Left(error)

      case Right(chain) =>
        // Get the queue of after-deploy commands and append them to the CommandChainOnSuccess
        // so that it will be traversed in a breath-first manner
        val cmdQueueFromConfig = getJobQueueFromConfig(configForCmd).map(
          gedCmdFromDeployJob(_, initialCmd)
        )
        val cmdQueueConcat = cmdQueue ++ cmdQueueFromConfig

        if (chainContainsCmd(chain, cmd)) {
          // this is here to prevent cyclic references
          Left(
            ConfigError(
              "Job appearing more than once in job chain",
              toFile(cmd, fmt)
            )
          )
        } else if (cmdQueueConcat.isEmpty) {
          Right(chain :+ (cmd, configForCmd))
        } else {
          val newChain = Right(chain :+ (cmd, configForCmd))
          val nextCmd = cmdQueueConcat.head

          getConfigFromCmd(nextCmd, fmt) match {
            case e: ConfigError => Left(e)
            case nextConfig: ValidConfig =>
              buildCmdChainOnSuccess(
                nextCmd,
                nextConfig,
                newChain,
                cmdQueueConcat.tail,
                initialCmd,
                fmt
              )
          }
        }
    }

  private def buildCmdOnFailure(
      initialConfig: ValidConfig,
      initialCmd: Cmd,
      fmt: Formatter
  ) = {
    getFailureJobFromConfig(initialConfig).map(
      gedCmdFromDeployJob(_, initialCmd)
    ) match {
      case None =>
        // No OnFailure job specified
        Right(None)

      case Some(cmd) =>
        getConfigFromCmd(cmd, fmt) match {
          case e: ConfigError      => Left(e)
          case config: ValidConfig => Right(Some(cmd, config))
        }
    }
  }

  /**
    * Execute the detected command for a valid configuration.
    */
  def executeCommand(
      cmd: Cmd,
      config: ValidConfig,
      fmt: Formatter
  ): CommandResult = {
    val cmdResult: CommandResult = cmd.action match {
      case ReleaseAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        ReleaseCommand(
          serviceConfig,
          fmt,
          isDryrun = cmd.isDryrun,
          deprecatedSoftGracePeriod = cmd.deprecatedSoftGracePeriod,
          deprecatedHardGracePeriod = cmd.deprecatedHardGracePeriod
        ).run()
      case ScaleAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        ScaleCommand(serviceConfig, fmt, isDryrun = cmd.isDryrun).run()
      case CheckAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        CheckCommand(
          serviceConfig,
          fmt,
          isDryrun = cmd.isDryrun,
          deprecatedSoftGracePeriod = cmd.deprecatedSoftGracePeriod,
          deprecatedHardGracePeriod = cmd.deprecatedHardGracePeriod
        ).run()
      case DockerEnvAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        DockerEnvCommand(
          serviceConfig,
          fmt,
          isDryrun = false
        ).run()
      case DockerRunAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        DockerRunCommand(
          serviceConfig,
          fmt,
          isDryrun = false
        ).run()

      case other =>
        // nothing to do.
        fmt.error(s"Action '$other' not implemented yet :( ")
        sys.exit(1)
    }

    cmdResult
  }

  def showConfigError(cmd: Cmd, configError: ConfigError, fmt: Formatter): Unit = {
    fmt.fmtBlock("Invalid config") {
      fmt.info(s""" Service Name: ${sanitizeServiceName(cmd.serviceName)}
           | Config File: ${configError.yamlFile.getAbsolutePath}
             """.stripMargin)
      fmt.error(configError.msg)
    }
    exitWithError()
  }

  val ConfigRepositoryEnvName = "NMESOS_CONFIG_REPOSITORY"

  def toFile(cmd: Cmd, fmt: Formatter): File = {
    sys.env.get(ConfigRepositoryEnvName) match {
      case None =>
        new File(s"${cmd.serviceName}.yml")
      case Some(path) =>
        fmt.println(s"$ConfigRepositoryEnvName: $path")
        new File(s"${path}${File.separator}${cmd.serviceName}.yml")
    }
  }

  def sanitizeServiceName(serviceName: String) = {
    serviceName.split(File.separatorChar).last
  }

  // Cli command config to shared config model
  def toServiceConfig(cmd: Cmd, config: ValidConfig) =
    CmdConfig(
      serviceName = sanitizeServiceName(cmd.serviceName),
      force = cmd.force,
      tag = cmd.tag,
      environment = config.environment,
      environmentName = config.environmentName,
      fileHash = config.fileHash,
      file = config.file
    )

  def exitWithError() = sys.exit(1)

  private def exit(fmt: Formatter, cmdResult: CommandResult): Unit = {
    cmdResult match {
      case CommandSuccess(msg) =>
        fmt.info(msg)

      case CommandError(error) =>
        fmt.error(error)
        exitWithError()
    }
  }

  private def getConfigFromCmd(cmd: Cmd, fmt: Formatter): ConfigResult = {
    val yamlFile = toFile(cmd, fmt)

    ConfigReader.parseEnvironment(yamlFile, cmd.environment, fmt) match {
      case error: ConfigError =>
        showConfigError(cmd, error, fmt)
        error

      case configForCmd: ValidConfig =>
        configForCmd
    }
  }

  private def chainContainsCmd(
      chain: List[(Cmd, ValidConfig)],
      cmd: Cmd
  ): Boolean =
    chain.exists { case (cmdInChain, _) => cmd == cmdInChain }

  private def getJobQueueFromConfig(config: ValidConfig): List[DeployJob] =
    config.environment.after_deploy match {
      case None              => List()
      case Some(afterDeploy) => afterDeploy.on_success
    }

  private def getFailureJobFromConfig(config: ValidConfig): Option[DeployJob] =
    config.environment.after_deploy match {
      case None              => None
      case Some(afterDeploy) => afterDeploy.on_failure
    }

  private def gedCmdFromDeployJob(job: DeployJob, initialCmd: Cmd): Cmd =
    initialCmd.copy(
      serviceName = job.service_name,
      tag = job.tag.getOrElse(initialCmd.tag),
      force = true
    )

  private def maybeExecuteFailureCommandAndExit(
      onFailure: Option[CommandAndConfig],
      fmt: Formatter
  ): Unit = {
    for ((failureCmd, failureConfig) <- onFailure) {
      executeCommand(failureCmd, failureConfig, fmt) match {
        case CommandSuccess(msg) =>
          fmt.info(s"Executed failure command: ${msg}")

        case CommandError(error) =>
          fmt.error(error)
      }
    }
    exitWithError()
  }
}
