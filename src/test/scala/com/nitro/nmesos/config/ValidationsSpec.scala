package com.nitro.nmesos.config

import org.specs2.mutable.Specification
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.{util => ju}
import _root_.java.{util => ju}
import _root_.java.{util => ju}

/*
 * Test the (basic) Validations
 */
class ValidationsSpec extends Specification {

  "Validations checkResources" should {

    "succeed when there is enough memory" in {
      val expectedResult = Seq(Ok("Resources - Memory Instances"))
      Validations.checkResources(
        model.Resources(0.0, 1, None)
      ) should be equalTo expectedResult
    }

    "fail when there is not enough memory" in {
      val expectedResult =
        Seq(Fail("Resources - Memory Instances", "Must be > 0"))
      Validations.checkResources(
        model.Resources(0.0, 0, None)
      ) should be equalTo expectedResult
      Validations.checkResources(
        model.Resources(0.0, -1, None)
      ) should be equalTo expectedResult
    }

  }

  "Validations checkContainers" should {

    "succeed when there are labels and env_vars" in {
      val container = model.Container(
        "",
        None,
        None,
        None,
        Some(Map("label" -> "red")),
        Some(Map("env" -> "var")),
        None,
        None,
        None,
        None
      )
      val expectedResult =
        Seq(Ok("Container - Labels"), Ok("Container - Environment vars"))
      Validations.checkContainer(container) should be equalTo expectedResult
    }

    "warn when there are no labels and/or no env_vars" in {
      val container = model.Container(
        "",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
      val expectedResult = Seq(
        Warning("Container - Labels", "No labels defined"),
        Warning("Container - Environment vars", "No env vars defined")
      )
      Validations.checkContainer(container) should be equalTo expectedResult
    }

  }

  "Validations checkScheduledJob" should {

    "skip/ignore when it is not a scheduled job" in {
      val resources = model.Resources(0.0, 0, None)
      val container = model.Container(
        "",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
      val singularity = model.SingularityConf(
        "",
        None,
        Some("SERVICE"),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
      val env = model.Environment(resources, container, None, singularity, None)
      Validations.checkScheduledJob(env) should be empty
    }

  }

  "Validations checkService" should {

    "skip/ignore when it is not a service" in {
      val resources = model.Resources(0.0, 0, None)
      val container = model.Container(
        "",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
      val singularity = model.SingularityConf(
        "",
        None,
        Some("SCHEDULED"),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
      val env = model.Environment(resources, container, None, singularity, None)
      Validations.checkService(env) should be empty
    }

  }

  "Validations checkDeprecated" should {

    "succeed if no env_vars are expired" in {
      val today = LocalDate.parse(
        "30-Jan-2020",
        DateTimeFormatter.ofPattern("dd-MMM-yyyy")
      )
      val expectedResult = Seq(
        Ok("Deprecated Env Var - OLD_ENV_VAR_10"),
        Ok("Deprecated Env Var - OLD_ENV_VAR_20"),
        Ok("Deprecated Env Var - OLD_ENV_VAR_30")
      )
      Validations.checkDeprecated(
        deprecatedEnvVarFile(),
        today,
        (21, 21)
      ) should be equalTo expectedResult
    }

    "fail/warn if env_vars are expired" in {
      val today = LocalDate.parse(
        "30-Jan-2020",
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", ju.Locale.US)
      )
      val expectedResult = Seq(
        Fail("Deprecated Env Var - OLD_ENV_VAR_10", "< 20"),
        Warning("Deprecated Env Var - OLD_ENV_VAR_20", "< 10"),
        Ok("Deprecated Env Var - OLD_ENV_VAR_30")
      )
      Validations.checkDeprecated(
        deprecatedEnvVarFile(),
        today,
        (10, 20)
      ) should be equalTo expectedResult
    }

  }

  "Validations processLine" should {

    "succeed, if a well formed line needs to get processes" in {
      val line = "OLD_ENV_VAR_10: \"old value\" # @deprecated-on 10-Jan-2020"
      val expectedDate = LocalDate.parse(
        "10-Jan-2020",
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", ju.Locale.US)
      )
      Validations.processLine(
        line
      ) should be equalTo ("OLD_ENV_VAR_10", expectedDate)
    }

    "fail, if a badly formed line needs to get processes" in {
      val line =
        "OLD_ENV_VAR_10: \"old value\" # @deprecated-on-bad 10-Jan-2020"
      Validations.processLine(line) should throwA[scala.MatchError]
    }

    "fail, if a badly formed date needs to get processes" in {
      val line = "OLD_ENV_VAR_10: \"old value\" # @deprecated-on 10-JAN-2020"
      Validations.processLine(line) should throwA[
        java.time.format.DateTimeParseException
      ]
    }
  }

  /*
   * The following env_vars are in the file ...
   * OLD_ENV_VAR_10: "old value" # @deprecated-on 10-Jan-2020
   * OLD_ENV_VAR_20: "old value" # @deprecated-on 20-Jan-2020
   * OLD_ENV_VAR_30: "old value" # @deprecated-on 30-Jan-2020
   */
  private def deprecatedEnvVarFile(): java.io.File = {
    new java.io.File(
      getClass
        .getResource("/config/example-config-with-deprecated-env-vars.yml")
        .getPath
    )
  }
}
