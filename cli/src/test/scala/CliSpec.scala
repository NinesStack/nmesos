package com.nitro.nmesos.cli

import java.io.File

import com.nitro.nmesos.cli.model.{ Cmd, ReleaseAction }
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.ValidConfig
import com.nitro.nmesos.util.{ InfoLogger }
import org.specs2.mutable.Specification

class CliSpec extends Specification with CliSpecFixtures {

  import org.specs2.execute._
  implicit def unitAsResult: AsResult[Unit] = new AsResult[Unit] {
    def asResult(u: => Unit): Result = { u; Success() }
  }

  "Cli main" should {

    "sanizied an serviceName that contains a file paths " in {
      val cmd = ValidCmd.copy(serviceName = "config/test")
      val cmdConfig = CliManager.toServiceConfig(cmd, ValidYamlConfig)

      cmdConfig.serviceName shouldEqual "test"
    }

    "build a valid command chain" in {
      val cmd = ValidCmd.copy(serviceName = "cli/src/test/resources/config/example-deploy-chain-service-0")

      val expectedCommandChainOnSuccess =
        List(
          (
            cmd,
            getValidYmlConfig("example-deploy-chain-service-0")),
          (
            cmd.copy(
              serviceName = "cli/src/test/resources/config/example-deploy-chain-service-1",
              tag = "job1tag",
              force = true),
            getValidYmlConfig("example-deploy-chain-service-1")),
          (
            cmd.copy(
              serviceName = "cli/src/test/resources/config/example-deploy-chain-service-2",
              tag = "job2tag",
              force = true),
            getValidYmlConfig("example-deploy-chain-service-2")),
          (
            cmd.copy(
              serviceName = "cli/src/test/resources/config/example-deploy-chain-service-3",
              tag = "job3tag",
              force = true),
            getValidYmlConfig("example-deploy-chain-service-3")))

      val expectedCommandOnFailure = (
        cmd.copy(
          serviceName = "cli/src/test/resources/config/example-deploy-chain-failure",
          tag = "jobFailureTag",
          force = true),
        getValidYmlConfig("example-deploy-chain-failure"))

      val (commandChainOnSuccess, commandOnFailure) = CliManager.getCommandChain(cmd, InfoLogger).right.get

      commandChainOnSuccess.zip(expectedCommandChainOnSuccess).foreach {
        case (command, expectedCommand) =>
          command._1 shouldEqual expectedCommand._1
          command._2.environment shouldEqual expectedCommand._2.environment
          command._2.environmentName shouldEqual expectedCommand._2.environmentName
          command._2.fileHash shouldEqual expectedCommand._2.fileHash
      }

      commandOnFailure.get._1 shouldEqual expectedCommandOnFailure._1
      commandOnFailure.get._2.environment shouldEqual expectedCommandOnFailure._2.environment
      commandOnFailure.get._2.environmentName shouldEqual expectedCommandOnFailure._2.environmentName
      commandOnFailure.get._2.fileHash shouldEqual expectedCommandOnFailure._2.fileHash
    }

    "return an error on a cyclic reference command chain" in {
      val cmd = ValidCmd.copy(serviceName = "cli/src/test/resources/config/example-deploy-chain-service-cyclic-0")
      val commandChain = CliManager.getCommandChain(cmd, InfoLogger)

      commandChain.left.get.msg shouldEqual "Job appearing more than once in job chain"
    }

    "return an error for a self referencing after deploy job" in {
      val cmd = ValidCmd.copy(serviceName = "cli/src/test/resources/config/example-deploy-chain-self-referencing")
      val commandChain = CliManager.getCommandChain(cmd, InfoLogger)

      commandChain.left.get.msg shouldEqual "Job appearing more than once in job chain"
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
    singularity = "",
    tag = "latest",
    force = false)

  lazy val ValidYamlConfig = {
    val yml = new File(getClass.getResource("/config/example-config.yml").getFile)
    val config = ConfigReader.parseEnvironment(yml, "dev", InfoLogger) match {
      case v: ValidConfig => Some(v)
      case _ => None
    }
    config.getOrElse(sys.error("Invalid yml"))
  }

  def getValidYmlConfig(serviceName: String): ValidConfig = {
    val yml = new File(getClass.getResource(s"/config/${serviceName}.yml").getFile)
    val config = ConfigReader.parseEnvironment(yml, "dev", InfoLogger) match {
      case v: ValidConfig => Some(v)
      case _ => None
    }
    config.getOrElse(sys.error("Invalid yml"))

  }
}
