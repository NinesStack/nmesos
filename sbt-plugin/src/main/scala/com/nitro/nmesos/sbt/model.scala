package com.nitro.nmesos.sbt

/**
  * CLI Sbt input model
  */
object model {

  sealed trait SbtCommandArgs

  case class ReleaseArgs(
      isDryrun: Boolean,
      verbose: Boolean,
      environment: String,
      tag: String,
      force: Boolean
  ) extends SbtCommandArgs

  object DefaultValues {
    val IsDryRun = true
    val Verbose = false
  }

}
