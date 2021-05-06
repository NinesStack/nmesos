package ninesstack.nmesos.config

import ninesstack.nmesos.config.model._
import ninesstack.nmesos.util.Formatter
import java.{util => ju, text => jt}
import java.time.{temporal => jtt}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Basic model validations
object Validations extends ValidationHelper {

  def checkAll(
      localConfig: CmdConfig,
      gracePeriods: (Int, Int)
  ): Seq[Validation] =
    Seq(
      checkResources(localConfig.environment.resources),
      checkContainer(localConfig.environment.container),
      checkScheduledJob(localConfig.environment),
      checkService(localConfig.environment),
      checkDeprecated(localConfig.file, LocalDate.now(), gracePeriods)
    ).flatten.sortBy(_.name)

  def checkResources(resources: Resources): Seq[Validation] =
    Seq(check("Resources - Memory Instances", "Must be > 0") {
      resources.memoryMb > 0
    })

  def checkService(env: Environment): Seq[Validation] =
    if (isService(env)) {
      Seq(
        check("Resources - Num Instances", "Must be > 0") {
          env.singularity.slavePlacement.exists(
            _ == "SPREAD_ALL_SLAVES"
          ) || env.resources.instances.exists(_ > 0)
        },
        checkWarning("Container - Ports", "Should be defined in HOST mode") {
          env.container.network
            .exists(_ == "HOST") || !env.container.ports.toSet.flatten.isEmpty
        },
        check("Container - Port values", "Must be >= 0 and <= 65535") {
          env.container.ports.toSeq.flatten.forall(isValidPort)
        },
        check("Container - Network", "Unsupported network") {
          env.container.network match {
            case None                          => true
            case Some("HOST") | Some("BRIDGE") => true
            case _                             => false
          }
        },
        checkWarning("Singularity - Healthcheck", "No healthcheck defined") {
          env.singularity.healthcheckUri.exists(_.trim.nonEmpty)
        }
      )
    } else {
      Seq.empty
    }

  def checkScheduledJob(env: Environment): Seq[Validation] =
    if (isScheduled(env)) {
      Seq(
        check("Resources - Num Instances", "Must be = 0") {
          env.resources.instances.isEmpty
        },
        check("Singularity - Job cron", "Missing") {
          env.singularity.schedule.isDefined
        }
      )
    } else {
      Seq.empty
    }

  def checkContainer(container: Container): Seq[Validation] =
    Seq(
      checkWarning("Container - Labels", "No labels defined") {
        !container.labels.toSet.flatten.isEmpty
      },
      checkWarning("Container - Environment vars", "No env vars defined") {
        !container.env_vars.toSet.flatten.isEmpty
      }
    )

  def checkDeprecated(
      file: java.io.File,
      today: LocalDate,
      gracePeriods: (Int, Int)
  ): Seq[Validation] = {
    val f = scala.io.Source.fromFile(file)
    val deprecated = f
      .getLines()
      .filterNot(_.trim().startsWith("#"))
      .filter(_.contains("@deprecated-on"))
      .map(processLine)
    deprecated.map(processDeprecated(_, today, gracePeriods)).toSeq
  }

  def isService(env: Environment) = {
    (env.singularity.requestType.isEmpty && env.singularity.schedule.isEmpty) || env.singularity.requestType == Option(
      "SERVICE"
    )
  }

  def isScheduled(env: Environment) = {
    (env.singularity.requestType.isEmpty && env.singularity.schedule.isDefined) || env.singularity.requestType == Option(
      "SCHEDULED"
    )
  }

  // OLD_ENV_VAR_10: "old value" # @deprecated-on 10-Jan-2020
  def processLine(line: String): (String, LocalDate) = {
    val p = raw"^\s*([A-Z0-9_]+):.*# @deprecated-on (.*)".r
    val p(envVar, date) = line
    val deprecatedOn = LocalDate.parse(
      date,
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", ju.Locale.US)
    )
    (envVar, deprecatedOn)
  }

  def processDeprecated(
      deprecated: (String, LocalDate),
      today: LocalDate,
      gracePeriods: (Int, Int)
  ): Validation = {
    val (envVar, deprecatedOn) = deprecated
    val (deprecatedSoftGracePeriod, deprecatedHardGracePeriod) = gracePeriods
    check(
      s"Deprecated Env Var - ${envVar}",
      s"< ${deprecatedHardGracePeriod}"
    ) {
      jtt.ChronoUnit.DAYS
        .between(deprecatedOn, today) < deprecatedHardGracePeriod
    } match {
      case Ok(_) =>
        checkWarning(
          s"Deprecated Env Var - ${envVar}",
          s"< ${deprecatedSoftGracePeriod}"
        ) {
          jtt.ChronoUnit.DAYS
            .between(deprecatedOn, today) < deprecatedSoftGracePeriod
        }
      case error =>
        error
    }
  }
}

trait ValidationHelper {

  def check(name: String, msg: String)(condition: => Boolean): Validation =
    if (condition) {
      Ok(name)
    } else {
      Fail(name, msg)
    }

  def checkWarning(name: String, msg: String)(
      condition: => Boolean
  ): Validation =
    if (condition) {
      Ok(name)
    } else {
      Warning(name, msg)
    }

  def fmtResult(checks: Seq[Validation], fmt: Formatter) =
    checks.foreach {
      case Ok(name) => fmt.println(s""" - ${fmt.infoColor("[OK]")}: $name""")
      case Warning(name, msg) =>
        fmt.println(s""" - ${fmt.errorColor("[Warning]")}: $name - $msg""")
      case Fail(name, msg) =>
        fmt.println(s""" - ${fmt.errorColor("[Failure]")}: $name - $msg""")
    }

  def isValidPort(port: PortMap): Boolean =
    isValidPort(port.containerPort) && port.hostPort
      .map(isValidPort)
      .getOrElse(true)

  def isValidPort(port: Int): Boolean = 0 <= port && port <= 65535

}

sealed trait Validation {
  def name: String
}

case class Ok(name: String) extends Validation

case class Warning(name: String, error: String) extends Validation

case class Fail(name: String, error: String) extends Validation
