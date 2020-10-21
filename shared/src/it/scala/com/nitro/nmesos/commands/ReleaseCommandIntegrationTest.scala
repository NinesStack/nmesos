package com.nitro.nmesos.commands

import java.io.File

import com.nitro.nmesos.singularity.ModelConversions
import com.nitro.nmesos.util.{CustomLogger, InfoLogger, Logger}
import com.nitro.nmesos.config.ConfigReader
import com.nitro.nmesos.config.ConfigReader.ValidConfig
import com.nitro.nmesos.config.model.CmdConfig
import org.specs2.mutable.Specification

/**
  * This integration test needs a clean Singularity running in dev at http://localhost:7099/singularity
  * To run Singularity locally:
  * cd $SINGULARITY_PATH
  * docker-compose rm
  * docker-compose up
  */
class ReleaseCommandIntegrationTest extends ReleaseCommandFixtures {
  sequential


  "Deploy Command" should {

    "Fail with a friendly message if it can't connect to Singularity" in {
      val command = ReleaseCommand(
        serviceConfigWithInvalidApi("example-service"),
        InfoLogger,
        isDryrun = true,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      )
      val result = command.run()

      val ExpectedError = CommandError("Unable to connect to http://invalid-api")
      result must be equalTo ExpectedError
    }

    "Show nice logs in dryrun when deploying a new service" in {
      val serviceConfig = buildConfig("example-service")
      val logger = new DummyLogger

      val command = ReleaseCommand(
        serviceConfig,
        logger,
        isDryrun = true,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      )
      val result = command.run()

      result must be equalTo CommandSuccess("Successfully deployed to 1 instances. [dry-run true] use --dry-run false")

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
           | - [OK]: Container - Environment vars
           | - [OK]: Container - Labels
           | - [OK]: Container - Network
           | - [OK]: Container - Ports
           | - [OK]: Resources - Memory Instances
           | - [OK]: Resources - Num Instances
           | - [OK]: Singularity - Healthcheck
           |--------------------------------------------------------------------------------
           |
           |
           |Applying config! ---------------------------------------------------------------
           | No Mesos config found with id: 'dev_example_service'
           | [dryrun] Need to create a new Mesos service with id: dev_example_service, instances: 1
           | [dryrun] Need to deploy image 'hubspot/singularity-test-service:latest'
           | [dryrun] Deploy to apply:
           |           * deployId:  latest_${HashExampleService}
           |           * image:     hubspot/singularity-test-service:latest
           |           * instances: 1, slavePlacement: OPTIMISTIC
           |           * ports:     8080
           |           * requestId: dev_example_service
           |           * resources: [cpus: 0.1, memory: 128.0Mb, role: *]
           |--------------------------------------------------------------------------------""".stripMargin

      compareOutput(ExpectedOutput, logger)
    }


    "deploy a example service for the first time with an expected log" in {


      val serviceConfig = buildConfig("example-service").copy(serviceName = ServiceNameInThisTest)

      val logger = new DummyLogger
      val command = ReleaseCommand(
        serviceConfig,
        logger,
        isDryrun = false,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      )
      val result = command.run()

      result must be equalTo CommandSuccess("Successfully deployed to 1 instances.")

      val ExpectedRequestId = ModelConversions.toSingularityRequestId(serviceConfig)

      val ExpectedOutput =
        s"""Deploying Config ---------------------------------------------------------------
           | Service Name: ${ServiceNameInThisTest}
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
           | - [OK]: Container - Environment vars
           | - [OK]: Container - Labels
           | - [OK]: Container - Network
           | - [OK]: Container - Ports
           | - [OK]: Resources - Memory Instances
           | - [OK]: Resources - Num Instances
           | - [OK]: Singularity - Healthcheck
           |--------------------------------------------------------------------------------
           |
           |
           |Applying config! ---------------------------------------------------------------
           | No Mesos config found with id: '${ExpectedRequestId}'
           | Created new Mesos service with Id: ${ExpectedRequestId}, instances: 1, state: ACTIVE
           | Deploying version 'hubspot/singularity-test-service:latest'
           | Deploy applied:
           |   * deployId:  latest_${HashExampleService}
           |   * image:     hubspot/singularity-test-service:latest
           |   * instances: 1, slavePlacement: OPTIMISTIC
           |   * ports:     8080
           |   * requestId: ${ExpectedRequestId}
           |   * resources: [cpus: 0.1, memory: 128.0Mb, role: *]
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
      val serviceConfig = buildConfig("example-service").copy(serviceName = ServiceNameInThisTest)
      val command = ReleaseCommand(
        serviceConfig,
        InfoLogger,
        isDryrun = false,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      )
      val result = command.run()


      // After the first deploy we try to deploy again.
      val logger = new DummyLogger
      val command2 = ReleaseCommand(
        serviceConfig,
        logger,
        isDryrun = false,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      )
      val result2 = command2.run()
      val ExpectedRequestId = ModelConversions.toSingularityRequestId(serviceConfig)
      val ExpectedError = CommandError(s"Unable to deploy - There is already a deploy with id latest_${HashExampleService}, use --force to force the redeploy")
      result2 must be equalTo ExpectedError
    }

    // Example of a no standard project (using extra docker parameters)
    "deploy sidecar with an expected log" in {
      val serviceConfig = buildConfig("sidecar").copy(serviceName = s"test-sidecar-${System.currentTimeMillis}")

      val command = ReleaseCommand(
        serviceConfig,
        InfoLogger,
        isDryrun = false,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      )
      val result = command.run()

      result must be equalTo CommandSuccess("Successfully deployed to 1 instances.")
    }

    "deploy a job for the first time with an expected log" in {


      val serviceConfig = buildConfig("example-job").copy(serviceName = JobNameInThisTest) //.copy(force = true)//.

      val logger =  new DummyLogger
      val command = ReleaseCommand(
        serviceConfig,
        logger,
        isDryrun = false,
        deprecatedSoftGracePeriod = 10,
        deprecatedHardGracePeriod = 20
      )
      val result = command.run()

      result must be equalTo CommandSuccess("Successfully scheduled - cron: '*/5 * * * *'.")

      val ExpectedRequestId = ModelConversions.toSingularityRequestId(serviceConfig)

      val ExpectedOutput =
        s"""Deploying Config ---------------------------------------------------------------
           | Service Name: ${JobNameInThisTest}
           | Config File:  /config/example-service.yml
           | environment:  dev
           | dry-run:      false
           | force:        false
           | image:        busybox:latest
           | api:          $SingularityUrl
           |--------------------------------------------------------------------------------
           |
           |
           |Verifying ----------------------------------------------------------------------
           | - [OK]: Container - Environment vars
           | - [OK]: Container - Labels
           | - [OK]: Resources - Memory Instances
           | - [OK]: Resources - Num Instances
           | - [OK]: Singularity - Job cron
           |--------------------------------------------------------------------------------
           |
           |
           |Applying config! ---------------------------------------------------------------
           | No Mesos config found with id: '${ExpectedRequestId}'
           | Scheduled new Mesos job with Id: ${ExpectedRequestId}, state: ACTIVE
           | Deploying version 'busybox:latest'
           | Deploy applied:
           |   * deployId:  latest_${HashExampleJobConfigFile}
           |   * image:     busybox:latest
           |   * requestId: ${ExpectedRequestId}
           |   * resources: [cpus: 0.1, memory: 64.0Mb, role: *]
           |   * scheduled: */5 * * * *
           |--------------------------------------------------------------------------------
           |
           |
           |Mesos Tasks Info ---------------------------------------------------------------
           | Deploy progress at $SingularityUrl/request/${ExpectedRequestId}/deploy/latest_${HashExampleJobConfigFile}
           | Scheduled Mesos Job State: SUCCEEDED
           |   * History: $SingularityUrl/request/${ExpectedRequestId}
           |   * Cron:    '*/5 * * * *'
           |--------------------------------------------------------------------------------""".stripMargin

      compareOutput(ExpectedOutput, logger)
    }

  }
}


trait ReleaseCommandFixtures extends Specification {

  val JobNameInThisTest = s"integration-test-job-${System.currentTimeMillis}"
  val ServiceNameInThisTest = s"integration-test-${System.currentTimeMillis}"
  val SingularityUrl = "http://localhost:7099/singularity"

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


  val HashExampleService = "17d5246"
  val HashExampleJobConfigFile = "8d4c0de"

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
