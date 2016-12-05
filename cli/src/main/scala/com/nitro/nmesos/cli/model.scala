package com.nitro.nmesos.cli

/**
 * CLI input model
 */
object model {

  case class Cmd(
    isDryrun: Boolean,
    verbose: Boolean,
    serviceName: String,
    environment: String,
    tag: String,
    force: Boolean,
    action: Action
  )

  object DefaultValues {
    val IsDryRun = true
    val Verbose = false
  }

  sealed trait Action
  case object NilAction extends Action
  case object ReleaseAction extends Action
  case object ScaleAction extends Action

}
