package com.nitro.nmesos.cli

import com.nitro.nmesos.cli.model._
import com.nitro.nmesos.BuildInfo

/**
 * CLI parser from `args` to `Cmd`.
 * Usage:
 *  val cmds = Cli.parser(args)
 * Cli example:
 *  nmesos release service_name \
 *    --environment dev \
 *    --version 0.0.1 \
 *    --dry-run false \
 *    --deprecated-soft-grace-limit 10 \
 *    --deprecated-hard-grace-limit 10
 */
object CliParser {

  def parse(args: Array[String]): Option[Cmd] = {
    val nilCommand = Cmd(
      action = NilAction,
      isDryrun = DefaultValues.IsDryRun,
      verbose = DefaultValues.Verbose,
      isFormatted = DefaultValues.IsFormatted,
      serviceName = "",
      singularity = "",
      environment = "",
      tag = "",
      force = false,
      deprecatedSoftGraceLimit = DefaultValues.DeprecatedSoftGraceLimit,
      deprecatedHardGraceLimit = DefaultValues.DeprecatedHardGraceLimit)
    cmdParser.parse(args, nilCommand)
  }

  private val cmdParser = new scopt.OptionParser[Cmd]("nmesos") {
    opt[Unit]("verbose")
      .abbr("v")
      .action((_, c) => c.copy(verbose = true))
      .text("More verbose output")

    opt[Unit]("noformat")
      .abbr("d")
      .optional()
      .action((_, c) => c.copy(isFormatted = false))
      .text("Disable ansi codes in the output")

    help("help")
      .abbr("h")
      .text("prints this usage text")

    note("\n")

    cmd("release")
      .text(" Release the a new version of the service.\n Usage:  nmesos release example-service --environment dev --tag 0.0.1")
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
          .validate(tag => if (tag.isEmpty) Left("Tag is required") else Right(()))
          .action((input, params) => params.copy(tag = input)),

        opt[Unit]("force")
          .abbr("f")
          .text("Force action")
          .optional()
          .action((input, params) => params.copy(force = true)),

        opt[Boolean]("dryrun")
          .abbr("x")
          .text("Is this a dry run?")
          .optional()
          .action((input, params) => params.copy(isDryrun = input)),

        opt[Boolean]("dry-run")
          .abbr("n")
          .text("Is this a dry run?")
          .optional()
          .action((input, params) => params.copy(isDryrun = input)),

        opt[Int]("deprecated-soft-grace-limit")
          .abbr("s")
          .text("Number of days, before warning")
          .optional()
          .action((input, params) => params.copy(deprecatedSoftGraceLimit = input)),

        opt[Int]("deprecated-hard-grace-limit")
          .abbr("h")
          .text("Number of days, before error/abort")
          .optional()
          .action((input, params) => params.copy(deprecatedHardGraceLimit = input)))

    note("\n")

    cmd("scale")
      .text(" Update the Environment.\n Usage: nmesos scale service_name --environment dev")
      .required()
      .action((_, params) => params.copy(action = ScaleAction))
      .children(

        arg[String]("service-name")
          .text("Name of the service to scale")
          .required()
          .action((input, params) => params.copy(serviceName = input)),

        opt[String]("environment")
          .abbr("e")
          .text("The environment to use")
          .required()
          .action((input, params) => params.copy(environment = input)),

        opt[Boolean]("dryrun")
          .abbr("x")
          .text("Is this a dry run?")
          .optional()
          .action((input, params) => params.copy(isDryrun = input)),

        opt[Boolean]("dry-run")
          .abbr("n")
          .text("Is this a dry run?")
          .optional()
          .action((input, params) => params.copy(isDryrun = input)))

    note("\n")

    cmd("check")
      .text(" Check the environment conf without running it.\n Usage: nmesos check service_name --environment dev")
      .required()
      .action((_, params) => params.copy(action = CheckAction))
      .children(

        arg[String]("service-name")
          .text("Name of the service to verify")
          .required()
          .action((input, params) => params.copy(serviceName = input)),

        opt[String]("environment")
          .abbr("e")
          .text("The environment to verify")
          .required()
          .action((input, params) => params.copy(environment = input)),

        opt[Int]("deprecated-soft-grace-limit")
          .abbr("s")
          .text("Number of days, before warning")
          .optional()
          .action((input, params) => params.copy(deprecatedSoftGraceLimit = input)),

        opt[Int]("deprecated-hard-grace-limit")
          .abbr("h")
          .text("Number of days, before error/abort")
          .optional()
          .action((input, params) => params.copy(deprecatedHardGraceLimit = input)))

    note("\n")

    cmd("verify")
      .text(" Verify a complete Singularity server by comparing the expected Singularity state with the Mesos state and docker state\n Usage: nmesos verify --singularity http://url/singularity")
      .required()
      .action((_, params) => params.copy(action = VerifyAction))
      .children(
        opt[String]("singularity")
          .abbr("s")
          .text("The environment to verify")
          .required()
          .action((input, params) => params.copy(singularity = input)))

    checkConfig { cmd =>
      val availableCommands = commands.map(_.fullName).mkString("|")
      if (cmd.action != NilAction) success else failure(s"A command is required. $availableCommands\n")
    }

    override def showUsageOnError: Boolean = true
  }

}
