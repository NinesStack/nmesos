package ninesstack.nmesos.commands

import ninesstack.nmesos.config.model.CmdConfig
import ninesstack.nmesos.util.Formatter
import ninesstack.nmesos.config.{Fail, Validations, Warning}

/**
  * Verify the configuration without any external call.
  */
case class CheckCommand(
    localConfig: CmdConfig,
    fmt: Formatter,
    isDryrun: Boolean,
    deprecatedSoftGracePeriod: Int,
    deprecatedHardGracePeriod: Int
) extends BaseCommand {

  override protected def processCmd(): CommandResult = {
    fmt.fmtBlock("Checking...") {
      val checks = Validations.checkAll(
        localConfig,
        (deprecatedSoftGracePeriod, deprecatedHardGracePeriod)
      )
      val errors = checks.collect { case f: Fail => f }
      val warnings = checks.collect { case f: Warning => f }
      Validations.fmtResult(checks, fmt)

      if (errors.isEmpty)
        CommandSuccess(s"Valid config with ${warnings.size} warnings")
      else CommandError(s"${errors.size} errors found")
    }
  }

}
