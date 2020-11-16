package com.nitro.nmesos.commands

import java.io.File
import com.nitro.nmesos.config.YamlParser.ValidYaml
import com.nitro.nmesos.util.InfoLogger
import org.specs2.mutable.Specification

import com.nitro.nmesos.singularity.ModelConversions
import com.nitro.nmesos.util.{CustomLogger, InfoLogger, Logger}
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.ValidConfig
import com.nitro.nmesos.config.model.CmdConfig
import org.specs2.mutable.Specification
import sys.process._
import scala.language.postfixOps

import scala.io.Source

class RunLocalCommandSpec extends Specification {
  "Run Local Command" should {
    "Build a successful `docker run` command string" in {

      val commandString = RunLocalCommand(
        buildConfig(),
        InfoLogger,
        isDryrun = true,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      ).runLocalCmdString()

      commandString must be equalTo "docker run -i -p 4000:4000 -e MY_ENV_VAR_2=MY_ENV_VAR_2 -e MY_ENV_VAR_1=MY_ENV_VAR_1 hubspot/singularity-test-service:latest"
    }
  }

  private def buildConfig() = {
    val serviceName =
      "shared/src/test/resources/config/example-config-run-local.yml"
    val yamlFile = new File(serviceName)
    ConfigReader.parseEnvironment(yamlFile, "dev", InfoLogger) match {
      case validConfig: ValidConfig =>
        CmdConfig(
          serviceName,
          "latest",
          force = false,
          validConfig.environmentName,
          validConfig.environment,
          validConfig.fileHash,
          yamlFile
        )
      case other => sys.error(other.toString)
    }
  }

}
