package com.nitro.nmesos.config

import com.nitro.nmesos.config.model._
import com.nitro.nmesos.util.Logger

// Basic model validations
object Validations extends ValidationHelper {

  def checkAll(localConfig: CmdConfig): Seq[Validation] = Seq(
    checkResources(localConfig.environment.resources),
    checkContainer(localConfig.environment.container),
    if (localConfig.environment.singularity.schedule.isDefined) {
      checkJob(localConfig.environment)
    } else {
      checkService(localConfig.environment)
    }
  ).flatten.sortBy(_.name)

  private def checkResources(resources: Resources): Seq[Validation] = Seq(
    check("Resources - Memory Instances", "Must be > 0") {
      resources.memoryMb > 0
    }
  )

  private def checkService(env: Environment): Seq[Validation] = Seq(
    check("Resources - Num Instances", "Must be > 0") {
      env.singularity.slavePlacement.exists(_ == "SPREAD_ALL_SLAVES") || env.resources.instances.exists(_ > 0)
    },
    checkWarning("Container - Ports", "Should be defined in HOST mode") {
      env.container.network.exists(_ == "HOST") || !env.container.ports.toSet.flatten.isEmpty
    },
    check("Container - Port values", "Must be >= 0 and <= 65535") {
      env.container.ports match {
        case Some(ports) => ports.map(portMap => portMap.hostPort match {
          case Some(hostPort) => 0 <= portMap.containerPort && portMap.containerPort <= 65535 &&
            0 <= hostPort && hostPort <= 65535
          case None => 0 <= portMap.containerPort && portMap.containerPort <= 65535
        }).reduce(_ && _)
        case _ => true
      }
    },
    check("Container - Network", "Unsupported network") {
      env.container.network match {
        case None => true
        case Some("HOST") | Some("BRIDGE") => true
        case _ => false
      }
    },
    checkWarning("Singularity - Healthcheck", "No healthcheck defined") {
      env.singularity.healthcheckUri.exists(_.trim.nonEmpty)
    }
  )

  private def checkJob(env: Environment): Seq[Validation] = Seq(
    check("Resources - Num Instances", "Must be = 0") {
      env.resources.instances.isEmpty
    },
    check("Singularity - Job cron", "Missing") {
      env.singularity.schedule.isDefined
    }
  )

  private def checkContainer(container: Container): Seq[Validation] = Seq(
    checkWarning("Container - Labels", "No labels defined") {
      !container.labels.toSet.flatten.isEmpty
    },
    checkWarning("Container - Environment vars", "No env vars defined") {
      !container.env_vars.toSet.flatten.isEmpty
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
    case Ok(name) => log.println(s""" - ${log.infoColor("[OK]")}: $name""")
    case Warning(name, msg) => log.println(s""" - ${log.errorColor("[Warning]")}: $name - $msg""")
    case Fail(name, msg) => log.println(s""" - ${log.errorColor("[Failure]")}: $name - $msg""")
  }

}

sealed trait Validation {
  def name: String
}
case class Ok(name: String) extends Validation
case class Warning(name: String, error: String) extends Validation
case class Fail(name: String, error: String) extends Validation
