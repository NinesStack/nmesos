package com.nitro.nmesos.config

import com.nitro.nmesos.util.InfoLogger
import com.nitro.nmesos.config.YamlParser._
import com.nitro.nmesos.config.model.{ ExecutorConf, PortMap }
import org.specs2.mutable.Specification

import scala.io.Source

/**
 * Test Yaml config file
 */
class YmlSpec extends Specification with YmlTestFixtures {

  "Yml Parser" should {

    "fail while reading an invalid YML file" in {
      val ExpectedMessage = "Invalid yaml file at line 9, column: 1\n     WTF!\n     ^"
      YamlParser.parse(YamlInvalid, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
    }

    "fail while reading a valid YML file without version" in {
      val ExpectedMessage = "Parser error for field nmesos_version: YamlObject is missing required member 'nmesos_version'"
      YamlParser.parse(YamlWithoutVersion, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
    }

    "fail while reading a valid YML file without all the required config parameters" in {
      val ExpectedMessage = "Parser error for field nmesos_version: YamlObject is missing required member 'nmesos_version'"
      YamlParser.parse(YamlWithIncompleteConf, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
    }

    "return a valid config from a valid Yaml file" in {
      YamlParser.parse(YamlExampleValid, InfoLogger) should beAnInstanceOf[ValidYaml]
    }

    "return a valid config from a valid Yaml with optional singularity config missing" in {
      YamlParser.parse(YamlJobExampleValid, InfoLogger) should beAnInstanceOf[ValidYaml]
    }

    "Parse the executor configuration in a valid Yaml file" in {
      val conf = YamlParser.parse(YamlExampleExecutorEnvs, InfoLogger)
      conf should beAnInstanceOf[ValidYaml]

      val ExpectedConf = Some(ExecutorConf(
        customExecutorCmd = Some("/opt/mesos/executor.sh"),
        env_vars = Some(Map(
          "EXECUTOR_SIDECAR_DISCOVER" -> "false",
          "EXECUTOR_SIDECAR_BACKOFF" -> "20m"
        ))
      ))
      val modelConfig = conf.asInstanceOf[ValidYaml].config
      modelConfig.environments("dev").executor should be equalTo (ExpectedConf)

      modelConfig.environments("dev").singularity.requiredRole should be equalTo (Some("OPS"))
      modelConfig.environments("dev").singularity.slavePlacement should be equalTo (Some("SPREAD_ALL_SLAVES"))
    }

    "Parse the port config from a valid Yaml file" in {
      val parsedYaml = YamlParser.parse(YamlExamplePortConfig, InfoLogger)
      parsedYaml should beAnInstanceOf[ValidYaml]

      val conf = parsedYaml.asInstanceOf[ValidYaml].config
      conf.environments("dev").container.ports must beSome.which(_.map(element => element match {
        case Left(port) => port must beEqualTo(8080)
        case Right(portMap) => portMap must beEqualTo(PortMap(9000, 12000))
      }))
    }
  }
}

trait YmlTestFixtures {

  val YamlInvalid =
    """
      |common: &common
      |  image: gonitro/test
      |
      |  resources:
      |    instances: 2 # Number of instances to deploy
      |    cpus: 0.5
      |    memoryMb: 2048
      |    diskMb: 0
      | WTF!
      |
      |environments:
      |  dev:
      |    <<: *common
      |
    """.stripMargin

  val YamlWithoutVersion =
    """
      |
      |common: &common
      |  image: gonitro/test
      |
      |  resources:
      |    instances: 2 # Number of instances to deploy
      |    cpus: 0.5
      |    memoryMb: 2048
      |    diskMb: 0
      |
      |environments:
      |  dev:
      |    <<: *common
      |
    """.stripMargin

  val YamlWithIncompleteConf =
    """
      |
      |common: &common
      |  image: gonitro/test
      |
      |  resources:
      |    instances: 2 # Number of instances to deploy
      |    cpus: 0.5
      |    memoryMb: 2048
      |    diskMb: 0
      |
      |environments:
      |  dev:
      |    <<: *common
      |
      |  prod:
      |    # image and all other required parameters are missing here
    """.stripMargin

  def YamlExampleValid = Source.fromURL(getClass.getResource("/config/example-config.yml")).mkString

  def YamlExampleExecutorEnvs = Source.fromURL(getClass.getResource("/config/example-config-with-executor.yml")).mkString

  def YamlJobExampleValid = Source.fromURL(getClass.getResource("/config/example-without-optional-config.yml")).mkString

  def YamlExamplePortConfig = Source.fromURL(getClass.getResource("/config/example-port-config.yml")).mkString
}