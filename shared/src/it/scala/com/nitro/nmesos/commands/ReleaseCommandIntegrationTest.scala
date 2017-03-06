package com.nitro.nmesos.commands

import java.io.File

import com.nitro.nmesos.singularity.ModelConversions
import com.nitro.nmesos.util.Logger

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
class ReleaseCommandIntegrationTest extends ReleaseCommandFixtures {
  sequential


  "Deploy Command" should {

    "Fail with a friendly message if it can't connect to Singularity" in {
      val command = ReleaseCommand(serviceConfigWithInvalidApi("example-service"), InfoLogger, isDryrun = true)
      val result = command.run()

      val ExpectedError = CommandError("Unable to connect to http://invalid-api")
      result must be equalTo ExpectedError
    }

    "Show nice logs in dryrun when deploying a new service" in {
      val serviceConfig = buildConfig("example-service")
      val logger = new DummyLogger

      val command = ReleaseCommand(serviceConfig, logger, isDryrun = true)
      val result = command.run()

      result must be equalTo CommandSuccess("Successfully deployed to 1 instances. [dryrun true] use --dryrun false")

      val ExpectedOutput =
        s"""Deploying Config ---------------------------------------------------------------
           | Service Name: example-service
           | Config File:  /config/example-service.yml
           | environment:  dev
           | dry-run:      true
           | force:        false
           | image:        hubspot/singularity-test-service:latest
           | api:          $SingularityUrl
           |--------------------------------------------------------------------------------
           |
           |
           |Verifying ----------------------------------------------------------------------
           | -  Resources - Num Instances: [OK]
           | -  Resources - Memory Instances: [OK]
           | -  Container - Ports: [OK]
           | -  Container - Labels: [OK]
           | -  Container - Environment vars: [OK]
           | -  Container - Network: [OK]
           | -  Singularity - Healthcheck: [OK]
           |--------------------------------------------------------------------------------
           |
           |
           |Applying config! ---------------------------------------------------------------
           | No Mesos service found with id: 'dev_example_service'
           | [dryrun] Need to create a new Mesos service with id: dev_example_service, instances: 1
           | [dryrun] Need to deploy image 'hubspot/singularity-test-service:latest'
           | [dryrun] Deploy to apply:
           |           * requestId: dev_example_service
           |           * deployId:  latest_${HashExampleService}
           |           * image:     hubspot/singularity-test-service:latest
           |           * instances: 1
           |           * resources: [cpus: 0.1, memory: 128.0Mb]
           |           * ports:     8080
           |--------------------------------------------------------------------------------""".stripMargin

      compareOutput(ExpectedOutput, logger)
    }


    "deploy a example service for the first time with an expected log" in {


      val serviceConfig = buildConfig("example-service").copy(serviceName = serviceNameInThisTest)

      val logger = new DummyLogger
      val command = ReleaseCommand(serviceConfig, logger, isDryrun = false)
      val result = command.run()

      result must be equalTo CommandSuccess("Successfully deployed to 1 instances.")

      val ExpectedRequestId = ModelConversions.toSingularityRequestId(serviceConfig)

      val ExpectedOutput =
        s"""Deploying Config ---------------------------------------------------------------
           | Service Name: ${serviceNameInThisTest}
           | Config File:  /config/example-service.yml
           | environment:  dev
           | dry-run:      false
           | force:        false
           | image:        hubspot/singularity-test-service:latest
           | api:          $SingularityUrl
           |--------------------------------------------------------------------------------
           |
           |
           |Verifying ----------------------------------------------------------------------
           | -  Resources - Num Instances: [OK]
           | -  Resources - Memory Instances: [OK]
           | -  Container - Ports: [OK]
           | -  Container - Labels: [OK]
           | -  Container - Environment vars: [OK]
           | -  Container - Network: [OK]
           | -  Singularity - Healthcheck: [OK]
           |--------------------------------------------------------------------------------
           |
           |
           |Applying config! ---------------------------------------------------------------
           | No Mesos service found with id: '${ExpectedRequestId}'
           | Created new Mesos service with Id: ${ExpectedRequestId}, instances: 1, state: ACTIVE
           | Deploying version 'hubspot/singularity-test-service:latest'
           | Deploy applied:
           |   * requestId: ${ExpectedRequestId}
           |   * deployId:  latest_${HashExampleService}
           |   * image:     hubspot/singularity-test-service:latest
           |   * instances: 1
           |   * resources: [cpus: 0.1, memory: 128.0Mb]
           |   * ports:     8080
           |--------------------------------------------------------------------------------
           |
           |
           |Mesos Tasks Info ---------------------------------------------------------------
           | Deploy progress at $SingularityUrl/request/${ExpectedRequestId}/deploy/latest_${HashExampleService}
           | Deploy Mesos Deploy State: SUCCEEDED
           |   * TaskId: ${ExpectedRequestId}-latest_5791cbd-1480516012328-1-localhost-DEFAULT
           |     - localhost:31583  -> 8080
           |--------------------------------------------------------------------------------""".stripMargin

      compareOutput(ExpectedOutput, logger)
    }


    "Ask for '--force' when deploying an existing service with the same version." in {

      // create a new service (in case it doesn't exist already
      val serviceConfig = buildConfig("example-service").copy(serviceName = serviceNameInThisTest)
      val command = ReleaseCommand(serviceConfig, InfoLogger, isDryrun = false)
      val result = command.run()


      // After the first deploy we try to deploy again.
      val logger = new DummyLogger
      val command2 = ReleaseCommand(serviceConfig, logger, isDryrun = false)
      val result2 = command2.run()
      val ExpectedRequestId = ModelConversions.toSingularityRequestId(serviceConfig)
      val ExpectedError = CommandError(s"Unable to deploy - There is already a deploy with id latest_${HashExampleService}, use --force to force the redeploy")
      result2 must be equalTo ExpectedError
    }

    // Example of a no standard project (using extra docker parameters)
    "deploy sidecar with an expected log" in {
      val serviceConfig = buildConfig("sidecar").copy(serviceName = s"test-sidecar-${System.currentTimeMillis}")

      val command = ReleaseCommand(serviceConfig, InfoLogger, isDryrun = false)
      val result = command.run()

      result must be equalTo CommandSuccess("Successfully deployed to 1 instances.")
    }


  }
}


trait ReleaseCommandFixtures extends Specification {

  val serviceNameInThisTest = s"integration-test-${System.currentTimeMillis}"
  val SingularityUrl = "http://192.168.99.100:7099/singularity"

  // Same conf with invalid api
  def serviceConfigWithInvalidApi(serviceName: String) = {
    val serviceConfig = buildConfig(serviceName)
    serviceConfig.copy(
      environment = serviceConfig.environment.copy(
        singularity = serviceConfig.environment.singularity.copy(
          url = "http://invalid-api"
        )
      )
    )
  }


  val HashExampleService = "7970c4d"

  def buildConfig(serviceName: String) = {
    val yamlFile = new File(getClass.getResource(s"/config/$serviceName.yml").toURI)
    ConfigReader.parseEnvironment(yamlFile, "dev", InfoLogger) match {
      case validConfig: ValidConfig =>
        CmdConfig(serviceName, "latest", force = false, validConfig.environmentName, validConfig.environment, validConfig.fileHash, yamlFile)
      case other => sys.error(other.toString)
    }

  }

  class DummyLogger extends Logger {
    val ansiEnabled = false
    var buffer = Seq.empty[String]

    def debug(msg: => Any): Unit = {}

    override def println(msg: => Any): Unit = buffer :+= {
      Console.println(msg)
      msg.toString
    }

  }

  def compareOutput(expectedOutput: String, logger: DummyLogger) = {
    def isLineToIgnore(line: String): Boolean = {
      line.startsWith(" Config File:") || line.contains("TaskId:") || line.contains("localhost:")
    }


    val expectedLines = expectedOutput.stripMargin.split("\n").filterNot(isLineToIgnore)
    val outputLines = logger.buffer.mkString("\n").split("\n").toSeq.filterNot(isLineToIgnore)

    outputLines.zip(expectedLines).foreach { case (expectedLine, outputLine) =>
      println(s"Out:      [$outputLine]")
      println(s"Expected: [$expectedLine]")
      expectedLine must be equalTo outputLine
    }
    outputLines must be equalTo expectedLines
  }


}