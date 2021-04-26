package com.nitro.nmesos.cli

import com.nitro.nmesos.cli.model._

/**
  * CLI parser from `args` to `Cmd`.
  * Usage:
  *  val cmds = Cli.parser(args)
  * Cli example:
  *  nmesos release service_name \
  *    --environment dev \
  *    --version 0.0.1 \
  *    --dry-run false \
  *    --deprecated-soft-grace-period 10 \
  *    --deprecated-hard-grace-period 20
  */
object CliParser {

  def parse(args: Array[String]): Option[Cmd] = {
    val nilCommand = Cmd(
      action = NilAction,
      isDryrun = DefaultValues.IsDryRun,
      isFormatted = DefaultValues.IsFormatted,
      serviceName = "",
      singularity = "",
      environment = "",
      tag = "",
      force = false,
      deprecatedSoftGracePeriod = DefaultValues.DeprecatedSoftGracePeriod,
      deprecatedHardGracePeriod = DefaultValues.DeprecatedHardGracePeriod
    )
    cmdParser.parse(args, nilCommand)
  }

  private val cmdParser = new scopt.OptionParser[Cmd]("nmesos") {
    opt[Unit]("noformat")
      .abbr("d")
      .text("Disable ansi codes in the output")
      .optional()
      .action((_, c) => c.copy(isFormatted = false))

    help("help")
      .abbr("h")
      .text("prints this usage text")

    note("\n")

    cmd("version")
      .text(
        " Show nmesos version."
      )
      .required()
      .action((_, params) => params.copy(action = VersionAction))

    note("\n")

    cmd("release")
      .text(
        " Release the a new version of the service.\n" ++
          "Usage: nmesos release <service-name> --environment <env> --tag <tag>"
      )
      .required()
      .action((_, params) => params.copy(action = ReleaseAction))
      .children(
        arg[String]("service-name")
          .text("Name of the service to release")
          .required()
          .action((input, params) => params.copy(serviceName = input)),
        opt[String]("environment")
          .abbr("e")
          .text("The environment to use")
          .required()
          .action((input, params) => params.copy(environment = input)),
        opt[String]("tag")
          .abbr("t")
          .text("Tag/Version to release")
          .required()
          .validate(tag =>
            if (tag.isEmpty) Left("Tag is required")
            else Right(())
          )
          .action((input, params) => params.copy(tag = input)),
        opt[Unit]("force")
          .abbr("f")
          .text("Force action!!!}")
          .optional()
          .action((input, params) => params.copy(force = true)),
        opt[Boolean]("dryrun")
          .abbr("x")
          .text("Deprecated. Will be removed soon.")
          .optional()
          .action((input, params) => params.copy(isDryrun = input)),
        opt[Boolean]("dry-run")
          .abbr("n")
          .text(s"Is this a dry run? Default: ${DefaultValues.IsDryRun}")
          .optional()
          .action((input, params) => params.copy(isDryrun = input)),
        opt[Int]("deprecated-soft-grace-period")
          .abbr("S")
          .text(
            s"Number of days, before warning. Default: ${DefaultValues.DeprecatedSoftGracePeriod}"
          )
          .optional()
          .action((input, params) =>
            params.copy(deprecatedSoftGracePeriod = input)
          ),
        opt[Int]("deprecated-hard-grace-period")
          .abbr("H")
          .text(
            s"Number of days, before error/abort. Default: ${DefaultValues.DeprecatedHardGracePeriod}"
          )
          .optional()
          .action((input, params) =>
            params.copy(deprecatedHardGracePeriod = input)
          )
      )

    note("\n")

    cmd("check")
      .text(
        " Check the environment conf without running it.\n" ++
          "Usage: nmesos check <service-name> --environment <dev>"
      )
      .required()
      .action((_, params) => params.copy(action = CheckAction))
      .children(
        arg[String]("service-name")
          .text("Name of the service to check")
          .required()
          .action((input, params) => params.copy(serviceName = input)),
        opt[String]("environment")
          .abbr("e")
          .text("The environment to use")
          .required()
          .action((input, params) => params.copy(environment = input)),
        opt[Int]("deprecated-soft-grace-period")
          .abbr("S")
          .text(
            s"Number of days, before warning. Default: ${DefaultValues.DeprecatedSoftGracePeriod}"
          )
          .optional()
          .action((input, params) =>
            params.copy(deprecatedSoftGracePeriod = input)
          ),
        opt[Int]("deprecated-hard-grace-period")
          .abbr("H")
          .text(
            s"Number of days, before error/abort. Default: ${DefaultValues.DeprecatedHardGracePeriod}"
          )
          .optional()
          .action((input, params) =>
            params.copy(deprecatedHardGracePeriod = input)
          )
      )

    note("\n")

    cmd("verify")
      .text(
        " Verify a complete Singularity server by comparing " ++
          "the expected Singularity state with the Mesos state and docker state\n" ++
          "Usage: nmesos verify --singularity <url>"
      )
      .required()
      .action((_, params) => params.copy(action = VerifyAction))
      .children(
        opt[String]("singularity")
          .abbr("s")
          .text("The url to the Singularity server")
          .required()
          .action((input, params) => params.copy(singularity = input))
      )

    note("\n")

    cmd("docker-env")
      .text(
        " Create a <service-name>.env/docker-compose.<service-name>.yml file " ++
          "(to run a/the docker container locally).\n" ++
          "Usage: nmesos docker-env <service-name> --environment <env> --tag <tag>"
      )
      .required()
      .action((_, params) => params.copy(action = DockerEnvAction))
      .children(
        arg[String]("service-name")
          .text("Name of the service to create")
          .required()
          .action((input, params) => params.copy(serviceName = input)),
        opt[String]("environment")
          .abbr("e")
          .text("The environment to use")
          .required()
          .action((input, params) => params.copy(environment = input)),
        opt[String]("tag")
          .abbr("t")
          .text("Tag to use/create")
          .required()
          .validate(tag =>
            if (tag.isEmpty) Left("Tag is required")
            else Right(())
          )
          .action((input, params) => params.copy(tag = input))
      )

    note("\n")

    cmd("docker-run")
      .text(
        " Run a/the docker container locally.\n" ++
          "Usage: nmesos docker-run <service-name> --environment <dev> --tag <tag>"
      )
      .required()
      .action((_, params) => params.copy(action = DockerRunAction))
      .children(
        arg[String]("service-name")
          .text("Name of the service to start")
          .required()
          .action((input, params) => params.copy(serviceName = input)),
        opt[String]("environment")
          .abbr("e")
          .text("The environment to use")
          .required()
          .action((input, params) => params.copy(environment = input)),
        opt[String]("tag")
          .abbr("t")
          .text("Tag to start")
          .required()
          .validate(tag =>
            if (tag.isEmpty) Left("Tag is required")
            else Right(())
          )
          .action((input, params) => params.copy(tag = input))
      )

    checkConfig { cmd =>
      //val availableCommands = commands.map(_.fullName).mkString("|")
      val availableCommands =
        "version | check | release | scale | verify | docker-env | docker-run"
      if (cmd.action != NilAction) success
      else failure(s"A command is required: ${availableCommands}\n")
    }

    override def showUsageOnError: Option[Boolean] = Some(true)
  }

}
