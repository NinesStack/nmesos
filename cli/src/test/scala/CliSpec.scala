package com.nitro.nmesos.cli

import java.io.File

import com.nitro.nmesos.cli.model.{Cmd, ReleaseAction}
import com.nitro.nmesos.commands.ReleaseCommand
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.ValidConfig
import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.ModelConversions.toSingularityRequest
import com.nitro.nmesos.singularity.{RealSingularityManager, SingularityManager}
import com.nitro.nmesos.util.InfoLogger
import org.specs2.mutable.Specification

class CliSpec extends Specification with CliSpecFixtures {

  "Cli Main" should {

    "sanizied an serviceName that contains a file paths " in {
      val cmd = ValidCmd.copy(serviceName = "config/test")
      val cmdConfig = CliManager.toServiceConfig(cmd, ValidYamlConfig)

      cmdConfig.serviceName shouldEqual "test"
    }

    "build a valid command chain" in {
      val cmd = ValidCmd.copy(
        serviceName =
          "cli/src/test/resources/config/example-deploy-chain-service-0",
        tag = "specialTag"
      )

      val expectedCommandChainOnSuccess =
        List(
          (cmd, getValidYmlConfig("example-deploy-chain-service-0")),
          (
            cmd.copy(
              serviceName =
                "cli/src/test/resources/config/example-deploy-chain-service-1",
              tag = "job1tag",
              force = true
            ),
            getValidYmlConfig("example-deploy-chain-service-1")
          ),
          (
            cmd.copy(
              serviceName =
                "cli/src/test/resources/config/example-deploy-chain-service-2",
              tag = "specialTag",
              force = true
            ),
            getValidYmlConfig("example-deploy-chain-service-2")
          ),
          (
            cmd.copy(
              serviceName =
                "cli/src/test/resources/config/example-deploy-chain-service-3",
              tag = "job3tag",
              force = true
            ),
            getValidYmlConfig("example-deploy-chain-service-3")
          )
        )

      val expectedCommandOnFailure = (
        cmd.copy(
          serviceName =
            "cli/src/test/resources/config/example-deploy-chain-failure",
          tag = "jobFailureTag",
          force = true
        ),
        getValidYmlConfig("example-deploy-chain-failure")
      )

      val (commandChainOnSuccess, commandOnFailure) =
        CliManager.getCommandChain(cmd, InfoLogger).right.get

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

    "build a command chain with only one command" in {
      val cmd = ValidCmd.copy(
        serviceName =
          "cli/src/test/resources/config/example-deploy-chain-service-2",
        tag = "latest"
      )
      val (commandChainOnSuccess, commandOnFailure) =
        CliManager.getCommandChain(cmd, InfoLogger).right.get

      val expectedCommand = (
        cmd.copy(
          serviceName =
            "cli/src/test/resources/config/example-deploy-chain-service-2",
          tag = "latest"
        ),
        getValidYmlConfig("example-deploy-chain-service-2")
      )

      commandChainOnSuccess.length shouldEqual 1

      commandChainOnSuccess.head._1 shouldEqual expectedCommand._1
      commandChainOnSuccess.head._2.environment shouldEqual expectedCommand._2.environment
      commandChainOnSuccess.head._2.environmentName shouldEqual expectedCommand._2.environmentName
      commandChainOnSuccess.head._2.fileHash shouldEqual expectedCommand._2.fileHash
    }

    "return an error on a cyclic reference command chain" in {
      val cmd = ValidCmd.copy(serviceName =
        "cli/src/test/resources/config/example-deploy-chain-service-cyclic-0"
      )
      val commandChain = CliManager.getCommandChain(cmd, InfoLogger)

      commandChain.left.get.msg shouldEqual "Job appearing more than once in job chain"
    }

    "return an error for a self referencing after deploy job" in {
      val cmd = ValidCmd.copy(serviceName =
        "cli/src/test/resources/config/example-deploy-chain-self-referencing"
      )
      val commandChain = CliManager.getCommandChain(cmd, InfoLogger)

      commandChain.left.get.msg shouldEqual "Job appearing more than once in job chain"
    }
  }

  "Cli Parser" should {

    "parse a valid release command" in {
      val cmd = CliParser.parse(ValidCliArgs)
      cmd should be equalTo Some(ValidCmd)
    }

    "parse a valid release command (with long args)" in {
      val cmd = CliParser.parse(ValidCliArgsLong)
      cmd should be equalTo Some(ValidCmd)
    }

    "fail to parse an invalid release command" in {
      val cmd = CliParser.parse(InvalidCliArgs)
      cmd should be equalTo None
    }

  }

}

trait CliSpecFixtures {

  val ValidCliArgs =
    "release testService -v -d -e dev -t latest -S 10 -H 20 -f -n false".split(
      " "
    )
  val ValidCliArgsLong =
    "release testService --verbose --noformat --environment dev --tag latest --deprecated-soft-grace-period 10 --deprecated-hard-grace-period 20 --force --dry-run false"
      .split(" ")
  val InvalidCliArgs = "release test -e dev -t".split(" ")

  val ValidCmd = Cmd(
    action = ReleaseAction,
    isDryrun = false,
    verbose = true,
    isFormatted = false,
    serviceName = "testService",
    environment = "dev",
    singularity = "",
    tag = "latest",
    force = true,
    deprecatedSoftGracePeriod = 10,
    deprecatedHardGracePeriod = 20
  )

  lazy val ValidYamlConfig = {
    val yml = new File(
      getClass.getResource("/config/example-config.yml").getFile
    )
    val config = ConfigReader.parseEnvironment(yml, "dev", InfoLogger) match {
      case v: ValidConfig => Some(v)
      case _              => None
    }
    config.getOrElse(sys.error("Invalid yml"))
  }

  def getValidYmlConfig(serviceName: String): ValidConfig = {
    val yml = new File(
      getClass.getResource(s"/config/${serviceName}.yml").getFile
    )
    val config = ConfigReader.parseEnvironment(yml, "dev", InfoLogger) match {
      case v: ValidConfig => Some(v)
      case _              => None
    }
    config.getOrElse(sys.error("Invalid yml"))
  }
}
