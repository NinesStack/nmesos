package com.nitro.nmesos.config

import com.nitro.nmesos.config.model.{ CmdConfig, Container, Resources, SingularityConf }
import com.nitro.nmesos.util.Logger

// Basic model validations
object Validations extends ValidationHelper {

  def checkAll(localConfig: CmdConfig): Seq[Validation] = Seq(
    checkResources(localConfig.environment.resources),
    checkContainer(localConfig.environment.container),
    checkSingularityConf(localConfig.environment.singularity)
  ).flatten

  private def checkResources(resources: Resources): Seq[Validation] = Seq(
    check("Resources - Num Instances", "Must be > 0") {
      resources.instances > 0
    },
    check("Resources - Memory Instances", "Must be > 0") {
      resources.memoryMb > 0
    }
  )

  private def checkContainer(container: Container): Seq[Validation] = Seq(
    checkWarning("Container - Ports", "No ports defined") {
      !container.ports.toSet.flatten.isEmpty
    },
    checkWarning("Container - Labels", "No labels defined") {
      !container.labels.toSet.flatten.isEmpty
    },
    checkWarning("Container - Environment vars", "No env vars defined") {
      !container.env_vars.toSet.flatten.isEmpty
    },
    check("Container - Network", "Unsupported network") {
      container.network match {
        case None => true
        case Some("HOST") | Some("BRIDGE") => true
        case _ => false
      }
    }
  )

  private def checkSingularityConf(singularityConf: SingularityConf): Seq[Validation] = Seq(
    checkWarning("Singularity - Healthcheck", "No healthcheck defined") {
      singularityConf.healthcheckUri.exists(_.trim.nonEmpty)
    }
  )

}

trait ValidationHelper {

  def check(name: String, msg: String)(condition: => Boolean): Validation = if (condition) {
    Ok(name)
  } else {
    Fail(name, msg)
  }

  def checkWarning(name: String, msg: String)(condition: => Boolean): Validation = if (condition) {
    Ok(name)
  } else {
    Warning(name, msg)
  }

  def logResult(checks: Seq[Validation], log: Logger) = checks.foreach {
    case Ok(name) => log.println(s""" -  $name: ${log.importantColor("[OK]")}""")
    case Warning(name, msg) => log.println(s""" -  $name: ${log.importantColor("[Warning]")} $msg""")
    case Fail(name, msg) => log.println(s""" -  $name: ${log.importantColor("[Failure]")} $msg""")
  }

}

sealed trait Validation
case class Ok(name: String) extends Validation
case class Warning(name: String, error: String) extends Validation
case class Fail(name: String, error: String) extends Validation
