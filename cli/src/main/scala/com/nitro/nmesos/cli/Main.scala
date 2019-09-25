package com.nitro.nmesos.cli

import com.nitro.nmesos.BuildInfo
import com.nitro.nmesos.util.{ CustomLogger, InfoLogger, Logger }
import com.nitro.nmesos.commands.{ CheckCommand, CommandResult, ScaleCommand, VerifyEnvCommand }
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

    cmd.action match {
      case VerifyAction =>
        val result = VerifyEnvCommand(cmd.singularity, log).run()
        exit(log, result)
      case _ =>
        processYmlCommand(cmd, log)
    }
  }

  def processYmlCommand(initialCmd: Cmd, log: Logger) = {
    val commandChain = getCommandChain(initialCmd, log)

    commandChain match {
      case Left(_) => exitWithError()

      case Right(chain) =>
        val successful = chain._1
        val onFailure = chain._2

        for ((cmd, config) <- successful) {
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

  type CommandAndConfig = (Cmd, ValidConfig)

  type CommandChainOnSuccess = Either[ConfigError, List[CommandAndConfig]]

  type CommandOnFailure = Either[ConfigError, Option[CommandAndConfig]]

  type CommandChain = Either[ConfigError, (List[CommandAndConfig], Option[CommandAndConfig])]

  //  def getCommandChain(initialCmd: Cmd, log: Logger): CommandChain = {
  //    def buildChain(cmd: Cmd, chainEither: CommandChain, cmdQueue: List[Cmd]): CommandChain = {
  //      chainEither match {
  //        case Left(error) => Left(error)
  //
  //        case Right(chain) =>
  //          val successfulChain = chain._1
  //          val failureCommand = chain._2
  //
  //          val yamlFile = toFile(cmd, log)
  //
  //          ConfigReader.parseEnvironment(yamlFile, cmd.environment, log) match {
  //            case error: ConfigError =>
  //              showConfigError(cmd, error, log)
  //
  //              Left(error)
  //
  //            case configForCmd: ValidConfig =>
  //              // Get the queue of after-deploy commands and append them to the cmdQueue
  //              // so that it will be traversed in a breath-first manner
  //              val cmdQueueFromConfig = getJobQueueFromConfig(configForCmd).map(cmdFromDeployJob(_, initialCmd))
  //              val cmdQueueAll = cmdQueue ++ cmdQueueFromConfig
  //
  //              if (cmdQueueAll.isEmpty) {
  //                Right(chain :+ (cmd, configForCmd))
  //              } else if (chainContainsCmd(chain, cmdQueueAll.head)) {
  //                Left(ConfigError("Job appearing more than once in job chain", yamlFile))
  //              } else {
  //                val newChain = Right(chain :+ (cmd, configForCmd))
  //                buildChain(cmdQueueAll.head, newChain, cmdQueueAll.tail)
  //              }
  //
  //          }
  //      }
  //    }
  //
  //    buildChain(initialCmd, Right(List()), List())
  //  }

  /**
   * Parses and returns a valid chain of Commands and their corresponding Config file.
   * Exits on the first invalid command/config
   */
  def getCommandChain(initialCmd: Cmd, log: Logger): CommandChain = {
    def buildCmdChainOnSuccess(cmd: Cmd, configForCmd: ValidConfig, chain: CommandChainOnSuccess, cmdQueue: List[Cmd]): CommandChainOnSuccess = {
      chain match {
        case Left(error) => Left(error)

        case Right(chain) =>
          // Get the queue of after-deploy commands and append them to the CommandChainOnSuccess
          // so that it will be traversed in a breath-first manner
          val cmdQueueFromConfig = getJobQueueFromConfig(configForCmd).map(gedCmdFromDeployJob(_, initialCmd))
          val cmdQueueConcat = cmdQueue ++ cmdQueueFromConfig

          if (chainContainsCmd(chain, cmd)) {
            Left(ConfigError("Job appearing more than once in job chain", toFile(cmd, log)))
          } else if (cmdQueueConcat.isEmpty) {
            Right(chain :+ (cmd, configForCmd))
          } else {
            val newChain = Right(chain :+ (cmd, configForCmd))
            val nextCmd = cmdQueueConcat.head

            getConfigFromCmd(nextCmd, log) match {
              case Left(e) => Left(e)
              case Right(nextConfig) =>
                buildCmdChainOnSuccess(nextCmd, nextConfig, newChain, cmdQueueConcat.tail)
            }
          }
      }
    }

    def buildCmdOnFailure(initialConfig: ValidConfig) = {
      getFailureJobFromConfig(initialConfig).map(gedCmdFromDeployJob(_, initialCmd)) match {
        case None => Right(None)

        case Some(cmd) =>
          getConfigFromCmd(cmd, log) match {
            case Left(e) => Left(e)
            case Right(config) => Right(Some(cmd, config))
          }
      }
    }

    getConfigFromCmd(initialCmd, log) match {
      case Left(e) => Left(e)

      case Right(initialConfig) =>
        val cmdChainOnSuccess = buildCmdChainOnSuccess(initialCmd, initialConfig, Right(List()), List())
        val cmdOnFailure = buildCmdOnFailure(initialConfig)

        (cmdChainOnSuccess, cmdOnFailure) match {
          case (Left(e), _) => Left(e)
          case (_, Left(e)) => Left(e)

          case (Right(cmdChainOnSuccess), Right(cmdOnFailure)) =>
            Right((cmdChainOnSuccess, cmdOnFailure))
        }
    }
  }

  /**
   * Execute the detected command for a valid configuration.
   */
  def executeCommand(cmd: Cmd, config: ValidConfig, log: Logger): CommandResult = {
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

    cmdResult
  }

  def showConfigError(cmd: Cmd, configError: ConfigError, log: Logger): Unit = {
    log.logBlock("Invalid config") {
      log.info(
        s""" Service Name: ${sanitizeServiceName(cmd.serviceName)}
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
  def toServiceConfig(cmd: Cmd, config: ValidConfig) = CmdConfig(
    serviceName = sanitizeServiceName(cmd.serviceName),
    force = cmd.force,
    tag = cmd.tag,
    environment = config.environment,
    environmentName = config.environmentName,
    fileHash = config.fileHash,
    file = config.file)

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

  private def getConfigFromCmd(cmd: Cmd, log: Logger): Either[ConfigError, ValidConfig] = {
    val yamlFile = toFile(cmd, log)

    ConfigReader.parseEnvironment(yamlFile, cmd.environment, log) match {
      case error: ConfigError =>
        showConfigError(cmd, error, log)
        Left(error)

      case configForCmd: ValidConfig =>
        Right(configForCmd)
    }
  }

  private def chainContainsCmd(chain: List[(Cmd, ValidConfig)], cmd: Cmd): Boolean =
    chain.exists { case (cmdInChain, _) => cmd == cmdInChain }

  private def getJobQueueFromConfig(config: ValidConfig): List[DeployJob] =
    config.environment.afterDeploy match {
      case None => List()
      case Some(afterDeploy) => afterDeploy.onSuccess
    }

  private def getFailureJobFromConfig(config: ValidConfig): Option[DeployJob] =
    config.environment.afterDeploy match {
      case None => None
      case Some(afterDeploy) => afterDeploy.onFailure
    }

  private def gedCmdFromDeployJob(job: DeployJob, initialCmd: Cmd): Cmd =
    initialCmd.copy(
      serviceName = job.serviceName,
      tag = job.tag,
      force = true)

  private def maybeExecuteFailureCommandAndExit(onFailure: Option[CommandAndConfig], log: Logger): Unit = {
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
