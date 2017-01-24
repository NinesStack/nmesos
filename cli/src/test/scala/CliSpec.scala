package com.nitro.nmesos.cli

import java.io.File

import com.nitro.nmesos.cli.model.{ Cmd, ReleaseAction }
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.ValidConfig
import com.nitro.nmesos.util.InfoLogger
import org.specs2.mutable.Specification

class CliSpec extends Specification with CliSpecFixtures {

  "Cli main" should {

    "sanizied an serviceName that contains a file paths " in {
      val cmd = ValidCmd.copy(serviceName = "config/test")
      val cmdConfig = CliManager.toServiceConfig(cmd, ValidYamlConfig)

      cmdConfig.serviceName shouldEqual "test"
    }
  }

  "Cli Parser" should {

    "parse a valid release command" in {
      val cmd = CliParser.parse(ValidCliArgs)
      cmd should be equalTo Some(ValidCmd)
    }

    "Fail to parse an invalid release command" in {
      val cmd = CliParser.parse(InvalidCliArgs)
      cmd should be equalTo None
    }

  }

}

trait CliSpecFixtures {

  val ValidCliArgs = "release test -e dev -t latest".split(" ")
  val InvalidCliArgs = "release test -e dev -t".split(" ")

  val ValidCmd = Cmd(
    action = ReleaseAction,
    isDryrun = true,
    verbose = false,
    isFormatted = true,
    serviceName = "test",
    environment = "dev",
    tag = "latest",
    force = false
  )

  lazy val ValidYamlConfig = {
    val yml = new File(getClass.getResource("/config/example-config.yml").getFile)
    val config = ConfigReader.parseEnvironment(yml, "dev", InfoLogger) match {
      case v: ValidConfig => Some(v)
      case _ => None
    }
    config.getOrElse(sys.error("Invalid yml"))
  }

}
