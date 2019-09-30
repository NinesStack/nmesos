package com.nitro.nmesos.config

import com.nitro.nmesos.util.InfoLogger
import com.nitro.nmesos.config.YamlParser._
import com.nitro.nmesos.config.YamlParserHelper.YamlCustomProtocol._
import com.nitro.nmesos.config.model._
import net.jcazevedo.moultingyaml._
import org.specs2.mutable.Specification

import scala.io.Source

/**
 * Test Yaml config file
 */
class YmlSpec extends Specification with YmlTestFixtures {

  "Yml Parser" should {

    "fail while reading an invalid YML file" in {
      val ExpectedMessage = "Invalid yaml file at line 9, column: 1\n     WTF!\n     ^"
      YamlParser.parse(YamlInvalidRandom, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
    }

    "fail while reading a valid YML file without version" in {
      val ExpectedMessage = "Parser error for field nmesos_version: YamlObject is missing required member 'nmesos_version'"
      YamlParser.parse(YamlInvalidWithoutVersion, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
    }

    "fail while reading a valid YML file without all the required config parameters" in {
      val ExpectedMessage = "Parser error for field nmesos_version: YamlObject is missing required member 'nmesos_version'"
      YamlParser.parse(YamlInvalidWithIncompleteConf, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
    }

    "return a valid config from a valid Yaml file" in {
      YamlParser.parse(YamlExampleValid, InfoLogger) should beAnInstanceOf[ValidYaml]
    }

    "return a valid config from a valid Yaml with optional singularity config missing" in {
      YamlParser.parse(YamlJobExampleValid, InfoLogger) should beAnInstanceOf[ValidYaml]
    }

    "parse the executor configuration in a valid Yaml file" in {
      val conf = YamlParser.parse(YamlExampleExecutorEnvs, InfoLogger)
      conf should beAnInstanceOf[ValidYaml]

      val ExpectedConf = Some(ExecutorConf(
        customExecutorCmd = Some("/opt/mesos/executor.sh"),
        env_vars = Some(Map(
          "EXECUTOR_SIDECAR_DISCOVER" -> "false",
          "EXECUTOR_SIDECAR_BACKOFF" -> "20m"))))
      val modelConfig = conf.asInstanceOf[ValidYaml].config
      modelConfig.environments("dev").executor should be equalTo ExpectedConf

      modelConfig.environments("dev").singularity.requiredRole should be equalTo Some("OPS")
      modelConfig.environments("dev").singularity.slavePlacement should be equalTo Some("SPREAD_ALL_SLAVES")
    }

    "parse the mesos slaves attributes in a valid Yaml file" in {
      val conf = YamlParser.parse(YamlMesosAttributeConfig, InfoLogger)
      conf should beAnInstanceOf[ValidYaml]
    }

    "parse the port config from a valid Yaml file" in {
      val parsedYaml = YamlParser.parse(YamlExamplePortConfig, InfoLogger)
      parsedYaml should beAnInstanceOf[ValidYaml]

      val conf = parsedYaml.asInstanceOf[ValidYaml].config
      conf.environments("dev").container.ports must beSome.which(_.map(portMap => portMap.hostPort match {
        case Some(hostPort) => portMap should be equalTo PortMap(9000, Option(12000), None)
        case None => portMap.protocols match {
          case None => portMap.containerPort should be equalTo 8080
          case Some(protocols) => {
            portMap.containerPort should be equalTo 6060
            protocols mustEqual "udp,tcp"
          }
        }
      }))
    }

    "parse the port config from a valid Yaml file and re-serialize it to Yaml" in {
      val parsedYaml = YamlParser.parse(YamlExamplePortConfigAllVariations, InfoLogger)
      parsedYaml should beAnInstanceOf[ValidYaml]

      parsedYaml.asInstanceOf[ValidYaml].config.toYaml.prettyPrint mustEqual ExpectedYamlExamplePortConfig
    }

    "fail while parsing an invalid port specification" in {
      val ExpectedMessage = "Parser error for field environments/container/ports: Failed to deserialize the port specification"
      YamlParser.parse(YamlInvalidPortConfig, InfoLogger) mustEqual InvalidYaml(ExpectedMessage)
    }

    "fail while parsing an invalid port specification" in {
      val ExpectedMessage = "Parser error for field environments/container/ports: Failed to deserialize the port map specification"
      YamlParser.parse(YamlInvalidPortMapConfig, InfoLogger) mustEqual InvalidYaml(ExpectedMessage)
    }

    "parse the after deploy configuration in a valid Yaml file" in {
      val conf = YamlParser.parse(YamlExampleAfterDeploy, InfoLogger)
      conf should beAnInstanceOf[ValidYaml]

      val modelConfig = conf.asInstanceOf[ValidYaml].config
      val successJob1 = modelConfig.environments("dev").after_deploy.get.on_success.head
      val successJob2 = modelConfig.environments("dev").after_deploy.get.on_success.drop(1).head

      successJob1.service_name shouldEqual "job1"
      successJob1.tag shouldEqual Some("job1tag")

      successJob2.service_name shouldEqual "job2"
      successJob2.tag shouldEqual None

      val failureJob = modelConfig.environments("dev").after_deploy.get.on_failure.get

      failureJob.service_name shouldEqual "jobFailure"
      failureJob.tag shouldEqual Some("jobFailureTag")
    }
  }
}

trait YmlTestFixtures {
  val ExpectedYamlExamplePortConfig =
    """|nmesos_version: 0.0.1
      |environments:
      |  dev:
      |    resources:
      |      cpus: 0.1
      |      memoryMb: 64
      |      instances: 2
      |    container:
      |      image: hubspot/singularity-test-service
      |      ports:
      |      - 8080
      |      - 8081/udp
      |      - 8082/tcp,udp
      |      - 9000:12000
      |      - 9001:12001/udp
      |      - 9002:12002/tcp,udp
      |    singularity:
      |      url: http://192.168.99.100:7099/singularity
      |""".stripMargin

  def YamlInvalidRandom = Source.fromURL(getClass.getResource("/config/invalid-random.yml")).mkString

  def YamlInvalidWithoutVersion = Source.fromURL(getClass.getResource("/config/invalid-without-version.yml")).mkString

  def YamlInvalidWithIncompleteConf = Source.fromURL(getClass.getResource("/config/invalid-with-incomplete-conf.yml")).mkString

  def YamlExampleValid = Source.fromURL(getClass.getResource("/config/example-config.yml")).mkString

  def YamlExampleExecutorEnvs = Source.fromURL(getClass.getResource("/config/example-config-with-executor.yml")).mkString

  def YamlExampleAfterDeploy = Source.fromURL(getClass.getResource("/config/example-config-with-after-deploy.yml")).mkString

  def YamlJobExampleValid = Source.fromURL(getClass.getResource("/config/example-without-optional-config.yml")).mkString

  def YamlExamplePortConfig = Source.fromURL(getClass.getResource("/config/example-port-config.yml")).mkString

  def YamlExamplePortConfigAllVariations = Source.fromURL(getClass.getResource("/config/example-port-config-all-variations.yml")).mkString

  def YamlInvalidPortConfig = Source.fromURL(getClass.getResource("/config/invalid-port-config.yml")).mkString

  def YamlInvalidPortMapConfig = Source.fromURL(getClass.getResource("/config/invalid-port-map-config.yml")).mkString

  def YamlMesosAttributeConfig = Source.fromURL(getClass.getResource("/config/example-mesos-attribute-config.yml")).mkString
}