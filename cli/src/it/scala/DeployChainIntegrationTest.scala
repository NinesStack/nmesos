package com.nitro.nmesos.cli

import java.io.File

import com.nitro.nmesos.cli.model.{ Cmd, ReleaseAction }
import com.nitro.nmesos.commands.ReleaseCommand
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.ValidConfig
import com.nitro.nmesos.config.model.CmdConfig
import com.nitro.nmesos.util.InfoLogger
import org.specs2.mutable.Specification

/**
  * This integration test needs a clean Singularity running in dev at http://192.168.99.100:7099/singularity
  * To run Singularity locally:
  * cd $SINGULARITY_PATH
  * docker-compose rm
  * docker-compose up
  */
class DeployChainIntegrationTest extends Specification with DeployChainFixtures {

  // dirty hack so that the test doesn't scream the
  // "No implicits found for parameter evidence"
  // because the last line is not an assertion
  import org.specs2.execute._
  implicit def unitAsResult: AsResult[Unit] = new AsResult[Unit] {
    def asResult(u: => Unit): Result = { u; Success() }
  }

  "Cli main" should {

    "deploy a job with a command chain" in {
      val expectedDeployedServices = List(
        "dev_example_deploy_chain_service_real_0",
        "dev_example_deploy_chain_service_real_1",
        "dev_example_deploy_chain_service_real_2",
        "dev_example_deploy_chain_service_real_3"
      )

      // First file to deploy that has a deploy-chain on it
      val cmd = ValidCmd.copy(
        serviceName = "cli/src/it/resources/config/example-deploy-chain-service-real-0",
        isDryrun = false)

      // Kickoff
      CliManager.processCmd(cmd)

      // Check running singularity task and assert that our expected services are running
      val manager = getManager("example-deploy-chain-service-real-0")
      val runningTasks = manager.getSingularityActiveTasks().get
      for (runningTask <- runningTasks) {
        (expectedDeployedServices contains runningTask.taskId.requestId) shouldEqual true
      }
    }
  }

}

trait DeployChainFixtures {

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

  def getManager(serviceName: String) = {
    val serviceNameWithPath = s"cli/src/it/resources/config/${serviceName}"
    val yamlFile = new File(getClass.getResource(s"/config/${serviceName}.yml").toURI)
    val cmdConfig = ConfigReader.parseEnvironment(yamlFile, "dev", InfoLogger) match {
      case validConfig: ValidConfig =>
        CmdConfig(serviceNameWithPath, "latest", force = false, validConfig.environmentName, validConfig.environment, validConfig.fileHash, yamlFile)
      case other => sys.error(other.toString)
    }

    ReleaseCommand(cmdConfig, InfoLogger, isDryrun = false).manager
  }
}
