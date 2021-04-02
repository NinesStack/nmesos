package com.nitro.nmesos.cli

import java.io.File

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import com.nitro.nmesos.cli.model.DefaultValues
import com.nitro.nmesos.cli.model.{Cmd, ReleaseAction, DockerEnvAction}
import com.nitro.nmesos.commands.ReleaseCommand
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.ValidConfig
import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.singularity.ModelConversions.toSingularityRequest
import com.nitro.nmesos.singularity.{RealSingularityManager, SingularityManager}
import com.nitro.nmesos.util.InfoFormatter

class CliSpec extends AnyFlatSpec with should.Matchers with CliSpecFixtures {

  "Cli Main" should "sanizied an serviceName that contains a file paths " in {
    val cmd = ValidCmd.copy(serviceName = "config/test")
    val cmdConfig = CliManager.toServiceConfig(cmd, ValidYamlConfig)

    cmdConfig.serviceName should be("test")
  }

  it should "build a valid command chain" in {
    val cmd = ValidCmd.copy(
      serviceName =
        "src/test/resources/config/example-deploy-chain-service-0",
      tag = "specialTag"
    )

    val expectedCommandChainOnSuccess =
      List(
        (cmd, getValidYmlConfig("example-deploy-chain-service-0")),
        (
          cmd.copy(
            serviceName =
              "src/test/resources/config/example-deploy-chain-service-1",
            tag = "job1tag",
            force = true
          ),
          getValidYmlConfig("example-deploy-chain-service-1")
        ),
        (
          cmd.copy(
            serviceName =
              "src/test/resources/config/example-deploy-chain-service-2",
            tag = "specialTag",
            force = true
          ),
          getValidYmlConfig("example-deploy-chain-service-2")
        ),
        (
          cmd.copy(
            serviceName =
              "src/test/resources/config/example-deploy-chain-service-3",
            tag = "job3tag",
            force = true
          ),
          getValidYmlConfig("example-deploy-chain-service-3")
        )
      )

    val expectedCommandOnFailure = (
      cmd.copy(
        serviceName =
          "src/test/resources/config/example-deploy-chain-failure",
        tag = "jobFailureTag",
        force = true
      ),
      getValidYmlConfig("example-deploy-chain-failure")
    )

    val (commandChainOnSuccess, commandOnFailure) =
      CliManager.getCommandChain(cmd, InfoFormatter).getOrElse(fail("Unexpected Either"))

    commandChainOnSuccess.zip(expectedCommandChainOnSuccess).foreach {
      case (command, expectedCommand) =>
        command._1 should be(expectedCommand._1)
        command._2.environment should be(expectedCommand._2.environment)
        command._2.environmentName should be(expectedCommand._2.environmentName)
        command._2.fileHash should be(expectedCommand._2.fileHash)
    }

    commandOnFailure.get._1 should be(expectedCommandOnFailure._1)
    commandOnFailure.get._2.environment should be(
      expectedCommandOnFailure._2.environment
    )
    commandOnFailure.get._2.environmentName should be(
      expectedCommandOnFailure._2.environmentName
    )
    commandOnFailure.get._2.fileHash should be(
      expectedCommandOnFailure._2.fileHash
    )
  }

  it should "build a command chain with only one command" in {
    val cmd = ValidCmd.copy(
      serviceName =
        "src/test/resources/config/example-deploy-chain-service-2",
      tag = "latest"
    )
    val (commandChainOnSuccess, commandOnFailure) =
      CliManager.getCommandChain(cmd, InfoFormatter).getOrElse(fail("Unexpected Either"))

    val expectedCommand = (
      cmd.copy(
        serviceName =
          "src/test/resources/config/example-deploy-chain-service-2",
        tag = "latest"
      ),
      getValidYmlConfig("example-deploy-chain-service-2")
    )

    commandChainOnSuccess.length should be(1)

    commandChainOnSuccess.head._1 should be(expectedCommand._1)
    commandChainOnSuccess.head._2.environment should be(
      expectedCommand._2.environment
    )
    commandChainOnSuccess.head._2.environmentName should be(
      expectedCommand._2.environmentName
    )
    commandChainOnSuccess.head._2.fileHash should be(
      expectedCommand._2.fileHash
    )
  }

  it should "return an error on a cyclic reference command chain" in {
    val cmd = ValidCmd.copy(serviceName =
      "src/test/resources/config/example-deploy-chain-service-cyclic-0"
    )
    val commandChain = CliManager.getCommandChain(cmd, InfoFormatter)

    commandChain.swap.getOrElse(fail("Unexpected Either")).msg should be(
      "Job appearing more than once in job chain"
    )
  }

  it should "return an error for a self referencing after deploy job" in {
    val cmd = ValidCmd.copy(serviceName =
      "src/test/resources/config/example-deploy-chain-self-referencing"
    )
    val commandChain = CliManager.getCommandChain(cmd, InfoFormatter)

    commandChain.swap.getOrElse(fail("Unexpected Either")).msg should be(
      "Job appearing more than once in job chain"
    )
  }

  "Cli Parser" should "parse a valid release command" in {
    val cmd = CliParser.parse(ValidCliArgs)
    cmd should be(Some(ValidCmd))
  }

  it should "parse a valid release command (with long args)" in {
    val cmd = CliParser.parse(ValidCliArgsLong)
    cmd should be(Some(ValidCmd))
  }

  it should "fail to parse an invalid release command" in {
    val cmd = CliParser.parse(InvalidCliArgs)
    cmd should be(None)
  }

}

trait CliSpecFixtures {

  val testService = "testService"
  val ValidCliArgs =
    s"release ${testService} -d -e dev -t abcdef -S 10 -H 20 -f -n false"
      .split(" ")
  val ValidCliArgsLong =
    s"release ${testService} --noformat --environment dev --tag abcdef --deprecated-soft-grace-period 10 --deprecated-hard-grace-period 20 --force --dry-run false"
      .split(" ")
  val InvalidCliArgs =
    s"release ${testService} -e dev -t"
      .split(" ")
  val ValidCmd = Cmd(
    action = ReleaseAction,
    isDryrun = false,
    isFormatted = false,
    serviceName = testService,
    environment = "dev",
    singularity = "",
    tag = "abcdef",
    force = true,
    deprecatedSoftGracePeriod = 10,
    deprecatedHardGracePeriod = 20
  )

  val ValidDockerEnvCliArgs =
    s"docker-env ${testService} -e dev -t abcdef"
      .split(" ")
  val ValidDockerEnvCmd = Cmd(
    action = DockerEnvAction,
    isDryrun = DefaultValues.IsDryRun,
    isFormatted = DefaultValues.IsFormatted,
    serviceName = "testService",
    environment = "dev",
    singularity = "",
    tag = "abcdef",
    force = false,
    deprecatedSoftGracePeriod = DefaultValues.DeprecatedSoftGracePeriod,
    deprecatedHardGracePeriod = DefaultValues.DeprecatedHardGracePeriod
  )

  lazy val ValidYamlConfig = {
    val yml = new File(
      getClass.getResource("/config/example-config.yml").getFile
    )
    val config = ConfigReader.parseEnvironment(yml, "dev", InfoFormatter) match {
      case v: ValidConfig => Some(v)
      case _              => None
    }
    config.getOrElse(sys.error("Invalid yml"))
  }

  def getValidYmlConfig(serviceName: String): ValidConfig = {
    val yml = new File(
      getClass.getResource(s"/config/${serviceName}.yml").getFile
    )
    val config = ConfigReader.parseEnvironment(yml, "dev", InfoFormatter) match {
      case v: ValidConfig => Some(v)
      case _              => None
    }
    config.getOrElse(sys.error("Invalid yml"))
  }
}
