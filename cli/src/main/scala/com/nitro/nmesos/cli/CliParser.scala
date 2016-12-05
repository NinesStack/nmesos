package com.nitro.nmesos.cli

import com.nitro.nmesos.cli.model._
import com.nitro.nmesos.BuildInfo

/**
 * CLI parser from `args` to `Cmd`.
 * Usage:
 *  val cmds = Cli.parser(args)
 * Cli example:
 *  nmesos release service_name --environment dev --version 0.0.1 --dry-run false
 */
object CliParser {

  def parse(args: Array[String]): Option[Cmd] = {
    val nilCommand = Cmd(
      action = NilAction,
      isDryrun = DefaultValues.IsDryRun,
      verbose = DefaultValues.Verbose,
      serviceName = "",
      environment = "",
      tag = "",
      force = false
    )
    cmdParser.parse(args, nilCommand)
  }

  private val cmdParser = new scopt.OptionParser[Cmd]("nmesos") {
    head("nmesos", BuildInfo.version)

    opt[Unit]("verbose")
      .abbr("v")
      .action((_, c) => c.copy(verbose = true))
      .text("More verbose output")

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
          .validate(tag => if(tag.isEmpty) Left("Tag is required") else Right())
          .action((input, params) => params.copy(tag = input)),

        opt[Unit]("force")
          .abbr("f")
          .text("Force action")
          .optional()
          .action((input, params) => params.copy(force = true)),

        opt[Boolean]("dryrun")
          .abbr("n")
          .text("Is this a dry run?")
          .optional()
          .action((input, params) => params.copy(isDryrun = input))
      )

    note("\n")

    cmd("scale")
      .text(" Update the Environment.\n Usage: nmesos scale service_name --environment dev")
      .required()
      .action((_, params) => params.copy(action = ReleaseAction))
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

        opt[Boolean]("dry-run")
          .abbr("d")
          .text("Is this a dry run?")
          .optional()
          .action((input, params) => params.copy(isDryrun = input))

      )

    checkConfig { cmd =>
      val availableCommands = commands.map(_.fullName).mkString("|")
      if (cmd.action != NilAction) success else failure(s"A command is required. $availableCommands\n")
    }

    override def showUsageOnError: Boolean = true
  }

}
