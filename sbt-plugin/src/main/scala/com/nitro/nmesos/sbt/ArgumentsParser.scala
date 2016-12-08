package com.nitro.nmesos.sbt.cli

import com.nitro.nmesos.BuildInfo
import com.nitro.nmesos.sbt.model.{ DefaultValues, ReleaseArgs }

/**
 * SBT args parser from `args` to `Cmd`.
 * Usage:
 * val cmds = ArgumentsParser.parser(args)
 * Cli example:
 * nmesosRelease service_name --environment dev --tag 0.0.1 --dry-run false
 */
object ArgumentsParser {

  def parseReleaseArgs(args: Seq[String]): Option[ReleaseArgs] = {
    val nilCommand = ReleaseArgs(
      isDryrun = DefaultValues.IsDryRun,
      verbose = DefaultValues.Verbose,
      environment = "",
      tag = "",
      force = false
    )
    cmdParser.parse(args, nilCommand)
  }

  val cmdParser = new scopt.OptionParser[ReleaseArgs]("nmesos") {
    head("Release to Mesos", BuildInfo.version)

    opt[Unit]("verbose")
      .abbr("v")
      .action((_, c) => c.copy(verbose = true))
      .text("More verbose output")

    help("help")
      .abbr("h")
      .text("prints this usage text")

    note("\n")

    opt[String]("environment")
      .abbr("e")
      .text("The environment to use")
      .required()
      .action((input, params) => params.copy(environment = input))

    opt[String]("tag")
      .abbr("t")
      .text("Tag/Version to release")
      .required()
      .validate(tag => if (tag.isEmpty) Left("Tag is required") else Right())
      .action((input, params) => params.copy(tag = input))

    opt[Unit]("force")
      .abbr("f")
      .text("Force action")
      .optional()
      .action((input, params) => params.copy(force = true))

    opt[Boolean]("dryrun")
      .abbr("n")
      .text("Is this a dry run?")
      .optional()
      .action((input, params) => params.copy(isDryrun = input))

    note("\n")

    override def showUsageOnError: Boolean = true
  }

  def releaseExampleUsage = cmdParser.usageExample

}
