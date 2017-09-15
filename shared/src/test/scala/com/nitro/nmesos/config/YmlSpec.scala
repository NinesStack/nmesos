package com.nitro.nmesos.config

import com.nitro.nmesos.util.InfoLogger
import com.nitro.nmesos.config.YamlParser._
import com.nitro.nmesos.config.YamlParserHelper.YamlCustomProtocol._
import com.nitro.nmesos.config.model.{ ExecutorConf, PortMap }
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
          "EXECUTOR_SIDECAR_BACKOFF" -> "20m"
        ))
      ))
      val modelConfig = conf.asInstanceOf[ValidYaml].config
      modelConfig.environments("dev").executor should be equalTo (ExpectedConf)

      modelConfig.environments("dev").singularity.requiredRole should be equalTo (Some("OPS"))
      modelConfig.environments("dev").singularity.slavePlacement should be equalTo (Some("SPREAD_ALL_SLAVES"))
    }

    "parse the port config from a valid Yaml file" in {
      val parsedYaml = YamlParser.parse(YamlExamplePortConfig, InfoLogger)
      parsedYaml should beAnInstanceOf[ValidYaml]

      val conf = parsedYaml.asInstanceOf[ValidYaml].config
      conf.environments("dev").container.ports must beSome.which(_.map(portMap => portMap.hostPort match {
        case Some(hostPort) => portMap must beEqualTo(PortMap(9000, Option(12000)))
        case None => portMap.containerPort must beEqualTo(8080)
      }))

      conf.toYaml.prettyPrint must beEqualTo(ExpectedYamlExamplePortConfig)
    }

    "fail while parsing an invalid port specification" in {
      val ExpectedMessage = "Parser error for field environments/container/ports: Failed to deserialize port specification"
      YamlParser.parse(YamlInvalidPortConfig, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
    }

    "fail while parsing an invalid port specification" in {
      val ExpectedMessage = "Parser error for field environments/container/ports: Failed to deserialize port map specification"
      YamlParser.parse(YamlInvalidPortMapConfig, InfoLogger) should be equalTo InvalidYaml(ExpectedMessage)
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
      |      - 9000:12000
      |      - 8080
      |    singularity:
      |      url: http://192.168.99.100:7099/singularity
      |""".stripMargin

  def YamlInvalidRandom = Source.fromURL(getClass.getResource("/config/invalid-random.yml")).mkString

  def YamlInvalidWithoutVersion = Source.fromURL(getClass.getResource("/config/invalid-without-version.yml")).mkString

  def YamlInvalidWithIncompleteConf = Source.fromURL(getClass.getResource("/config/invalid-with-incomplete-conf.yml")).mkString

  def YamlExampleValid = Source.fromURL(getClass.getResource("/config/example-config.yml")).mkString

  def YamlExampleExecutorEnvs = Source.fromURL(getClass.getResource("/config/example-config-with-executor.yml")).mkString

  def YamlJobExampleValid = Source.fromURL(getClass.getResource("/config/example-without-optional-config.yml")).mkString

  def YamlExamplePortConfig = Source.fromURL(getClass.getResource("/config/example-port-config.yml")).mkString

  def YamlInvalidPortConfig = Source.fromURL(getClass.getResource("/config/invalid-port-config.yml")).mkString

  def YamlInvalidPortMapConfig = Source.fromURL(getClass.getResource("/config/invalid-port-map-config.yml")).mkString
}