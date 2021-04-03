package com.nitro.nmesos.cli

/**
  * CLI input model
  */
object model {

  case class Cmd(
      isDryrun: Boolean,
      isFormatted: Boolean,
      serviceName: String,
      environment: String,
      singularity: String,
      tag: String,
      force: Boolean,
      action: Action,
      deprecatedSoftGracePeriod: Int,
      deprecatedHardGracePeriod: Int
  )

  object DefaultValues {
    val IsDryRun = true
    val IsFormatted = true
    val DeprecatedSoftGracePeriod = 14
    val DeprecatedHardGracePeriod = 28
  }

  sealed trait Action
  case object NilAction extends Action
  case object VersionAction extends Action
  case object ReleaseAction extends Action
  case object ScaleAction extends Action
  case object CheckAction extends Action
  case object VerifyAction extends Action
  case object DockerEnvAction extends Action
  case object DockerRunAction extends Action

}
