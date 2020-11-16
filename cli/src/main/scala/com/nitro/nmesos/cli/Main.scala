package com.nitro.nmesos.cli

import com.nitro.nmesos.BuildInfo
import com.nitro.nmesos.commands.{
  CheckCommand,
  CommandResult,
  RunLocalCommand,
  ScaleCommand,
  VerifyEnvCommand
}
import com.nitro.nmesos.config.ConfigReader.ConfigResult
import com.nitro.nmesos.config.model.DeployJob

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
  import com.nitro.nmesos.commands.{CommandError, CommandSuccess}
  import com.nitro.nmesos.util.{Logger, CustomLogger}
  import com.nitro.nmesos.commands.ReleaseCommand
  import com.nitro.nmesos.config.ConfigReader
  import com.nitro.nmesos.config.ConfigReader.{ConfigError, ValidConfig}
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

    cmd.action match {
      case VerifyAction =>
        val result = VerifyEnvCommand(cmd.singularity, log).run()
        exit(log, result)
      case _ =>
        processYmlCommand(cmd, log)
    }
  }

  /**
    * Process all ymls in a deploy chain.
    * Log and exit on the first error
    */
  def processYmlCommand(initialCmd: Cmd, log: Logger) = {
    val commandChain = getCommandChain(initialCmd, log)

    commandChain match {
      case Left(error) =>
        log.error(error)
        exitWithError()

      case Right(chain) =>
        val onSuccess = chain._1
        val onFailure = chain._2

        for ((cmd, config) <- onSuccess) {
          if (config.environment.container.deploy_freeze.getOrElse(false)) {
            log.error(
              "Attention: deploy_freeze set to true. You're not able to deploy this config."
            )
            exitWithError()
          } else {
            executeCommand(cmd, config, log) match {
              case CommandSuccess(msg) =>
                log.info(msg)

              case CommandError(error) =>
                log.error(error)
                maybeExecuteFailureCommandAndExit(onFailure, log)
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
  def getCommandChain(initialCmd: Cmd, log: Logger): CommandChain = {
    getConfigFromCmd(initialCmd, log) match {
      case e: ConfigError => Left(e)

      case initialConfig: ValidConfig =>
        val cmdChainOnSuccess = buildCmdChainOnSuccess(
          initialCmd,
          initialConfig,
          Right(List()),
          List(),
          initialCmd,
          log
        )
        val cmdOnFailure = buildCmdOnFailure(initialConfig, initialCmd, log)

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
      log: Logger
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
              toFile(cmd, log)
            )
          )
        } else if (cmdQueueConcat.isEmpty) {
          Right(chain :+ (cmd, configForCmd))
        } else {
          val newChain = Right(chain :+ (cmd, configForCmd))
          val nextCmd = cmdQueueConcat.head

          getConfigFromCmd(nextCmd, log) match {
            case e: ConfigError => Left(e)
            case nextConfig: ValidConfig =>
              buildCmdChainOnSuccess(
                nextCmd,
                nextConfig,
                newChain,
                cmdQueueConcat.tail,
                initialCmd,
                log
              )
          }
        }
    }

  private def buildCmdOnFailure(
      initialConfig: ValidConfig,
      initialCmd: Cmd,
      log: Logger
  ) = {
    getFailureJobFromConfig(initialConfig).map(
      gedCmdFromDeployJob(_, initialCmd)
    ) match {
      case None =>
        // No OnFailure job specified
        Right(None)

      case Some(cmd) =>
        getConfigFromCmd(cmd, log) match {
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
      log: Logger
  ): CommandResult = {
    val cmdResult: CommandResult = cmd.action match {
      case ReleaseAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        ReleaseCommand(
          serviceConfig,
          log,
          isDryrun = cmd.isDryrun,
          deprecatedSoftGracePeriod = cmd.deprecatedSoftGracePeriod,
          deprecatedHardGracePeriod = cmd.deprecatedHardGracePeriod
        ).run()
      case ScaleAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        ScaleCommand(serviceConfig, log, isDryrun = cmd.isDryrun).run()
      case CheckAction =>
        val serviceConfig = toServiceConfig(cmd, config)
        CheckCommand(
          serviceConfig,
          log,
          isDryrun = cmd.isDryrun,
          deprecatedSoftGracePeriod = cmd.deprecatedSoftGracePeriod,
          deprecatedHardGracePeriod = cmd.deprecatedHardGracePeriod
        ).run()
      case RunLocalAction =>
        val serviceConfig = toServiceConfig(cmd, config)

        RunLocalCommand(
          serviceConfig,
          log,
          isDryrun = false,
          deprecatedSoftGracePeriod = cmd.deprecatedSoftGracePeriod,
          deprecatedHardGracePeriod = cmd.deprecatedHardGracePeriod
        ).run()

      case other =>
        // nothing to do.
        log.error(s"Action '$other' not implemented yet :( ")
        sys.exit(1)
    }

    cmdResult
  }

  def showConfigError(cmd: Cmd, configError: ConfigError, log: Logger): Unit = {
    log.logBlock("Invalid config") {
      log.info(s""" Service Name: ${sanitizeServiceName(cmd.serviceName)}
           | Config File: ${configError.yamlFile.getAbsolutePath}
             """.stripMargin)
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

  private def exit(log: Logger, cmdResult: CommandResult): Unit = {
    cmdResult match {
      case CommandSuccess(msg) =>
        log.info(msg)

      case CommandError(error) =>
        log.error(error)
        exitWithError()
    }
  }

  private def getConfigFromCmd(cmd: Cmd, log: Logger): ConfigResult = {
    val yamlFile = toFile(cmd, log)

    val environment = cmd.action match {
      case RunLocalAction => "dev"
      case _              => cmd.environment
    }

    ConfigReader.parseEnvironment(yamlFile, environment, log) match {
      case error: ConfigError =>
        showConfigError(cmd, error, log)
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
      log: Logger
  ): Unit = {
    for ((failureCmd, failureConfig) <- onFailure) {
      executeCommand(failureCmd, failureConfig, log) match {
        case CommandSuccess(msg) =>
          log.info(s"Executed failure command: ${msg}")

        case CommandError(error) =>
          log.error(error)
      }
    }
    exitWithError()
  }
}
