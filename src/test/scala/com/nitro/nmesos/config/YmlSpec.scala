package com.nitro.nmesos.config

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import com.nitro.nmesos.util.InfoLogger
import com.nitro.nmesos.config.YamlParser._
import com.nitro.nmesos.config.YamlParserHelper.YamlCustomProtocol._
import com.nitro.nmesos.config.model._
import net.jcazevedo.moultingyaml._

import scala.io.Source

/**
  * Test Yaml config file
  */
class YmlSpec extends AnyFlatSpec with should.Matchers with YmlTestFixtures {

  "Yml Parser" should "fail while reading an invalid YML file" in {
    val expectedMessage =
      "Invalid yaml file at line 9, column: 1\n     WTF!\n     ^"
    val parseResult = YamlParser.parse(
      YamlInvalidRandom,
      InfoLogger
    )
    parseResult should be(InvalidYaml(expectedMessage))
  }

  it should "fail while reading a valid YML file without version" in {
    val expectedMessage =
      "Parser error for field nmesos_version: YamlObject is missing required member 'nmesos_version'"
    val parseResult = YamlParser.parse(
      YamlInvalidWithoutVersion,
      InfoLogger
    )
    parseResult should be(InvalidYaml(expectedMessage))
  }

  it should "fail while reading a valid YML file without all the required config parameters" in {
    val expectedMessage =
      "Parser error for field : YAML object expected"
    val parseResult = YamlParser.parse(
      YamlInvalidWithIncompleteConf,
      InfoLogger
    )
    parseResult should be(InvalidYaml(expectedMessage))
  }

  it should "return a valid config from a valid Yaml file" in {
    YamlParser
      .parse(YamlExampleValid, InfoLogger) shouldBe a[ValidYaml]
  }

  it should "return a valid config from a valid Yaml with optional singularity config missing" in {
    YamlParser
      .parse(YamlJobExampleValid, InfoLogger) shouldBe a[ValidYaml]
  }

  it should "return a valid config from a valid Yaml with optional envar config being empty" in {
    YamlParser.parse(
      YamlEnvVarExampleValid,
      InfoLogger
    ) shouldBe a[ValidYaml]
  }

  it should "parse the executor configuration in a valid Yaml file" in {
    val conf = YamlParser.parse(YamlExampleExecutorEnvs, InfoLogger)
    conf shouldBe a[ValidYaml]

    val ExpectedConf = Some(
      ExecutorConf(
        customExecutorCmd = Some("/opt/mesos/executor.sh"),
        env_vars = Some(
          Map(
            "EXECUTOR_SIDECAR_DISCOVER" -> "false",
            "EXECUTOR_SIDECAR_BACKOFF" -> "20m"
          )
        )
      )
    )
    val modelConfig = conf.asInstanceOf[ValidYaml].config
    modelConfig.environments("dev").executor should be(ExpectedConf)

    modelConfig
      .environments("dev")
      .singularity
      .requiredRole should be(Some("OPS"))
    modelConfig
      .environments("dev")
      .singularity
      .slavePlacement should be(Some("SPREAD_ALL_SLAVES"))
  }

  it should "parse the mesos slaves required and allowed attributes in a valid Yaml file" in {
    val conf = YamlParser.parse(YamlMesosAttributeConfig, InfoLogger)
    conf shouldBe a[ValidYaml]
    val singularity =
      conf.asInstanceOf[ValidYaml].config.environments("dev").singularity
    singularity.requiredAttributes should be(
      Some(
        Map("mesosfunction" -> "master-prod", "Role" -> "docker-host")
      )
    )
    singularity.allowedSlaveAttributes should be(
      Some(
        Map("compute_class" -> "high_cpu")
      )
    )
  }

  it should "parse the port config from a valid Yaml file" in {
    val parsedYaml = YamlParser.parse(YamlExamplePortConfig, InfoLogger)
    parsedYaml shouldBe a[ValidYaml]

    val conf = parsedYaml.asInstanceOf[ValidYaml].config
    val expectedPorts = Some(List(
      PortMap(6060,None,Some("udp,tcp")),
      PortMap(8080,None,None),
      PortMap(9000,Some(12000),None)
    ))

    val ports = conf.environments("dev").container.ports
    ports should be(expectedPorts)
  }

  it should "parse the port config from a valid Yaml file and re-serialize it to Yaml" in {
    val parsedYaml =
      YamlParser.parse(YamlExamplePortConfigAllVariations, InfoLogger)
    parsedYaml shouldBe a[ValidYaml]

    parsedYaml
      .asInstanceOf[ValidYaml]
      .config
      .toYaml
      .prettyPrint should be(ExpectedYamlExamplePortConfig)
  }

  it should "fail while parsing an invalid port specification" in {
    val ExpectedMessage =
      "Parser error for field environments/container/ports: Failed to deserialize the port specification"
    YamlParser.parse(YamlInvalidPortConfig, InfoLogger) should be(
      InvalidYaml(
        ExpectedMessage
      )
    )
  }

  it should "parse the after deploy configuration in a valid Yaml file" in {
    val conf = YamlParser.parse(YamlExampleAfterDeploy, InfoLogger)
    conf shouldBe a[ValidYaml]

    val modelConfig = conf.asInstanceOf[ValidYaml].config
    val successJob1 =
      modelConfig.environments("dev").after_deploy.get.on_success.head
    val successJob2 =
      modelConfig.environments("dev").after_deploy.get.on_success.drop(1).head

    successJob1.service_name should be("job1")
    successJob1.tag should be(Some("job1tag"))

    successJob2.service_name should be("job2")
    successJob2.tag should be(None)

    val failureJob =
      modelConfig.environments("dev").after_deploy.get.on_failure.get

    failureJob.service_name should be("jobFailure")
    failureJob.tag should be(Some("jobFailureTag"))
  }

  it should "return a value for deploy_freeze" in {
    val parsed_deploy_freeze = YamlParser
      .parse(YamlExampleWithDeployFreeze, InfoLogger)
      .asInstanceOf[ValidYaml]
    val parsed_without_deploy_freeze = YamlParser
      .parse(YamlExampleWithoutDeployFreeze, InfoLogger)
      .asInstanceOf[ValidYaml]

    parsed_deploy_freeze.config.environments
      .get("dev")
      .get
      .container
      .deploy_freeze should be(Some(true))
    parsed_without_deploy_freeze.config.environments
      .get("dev")
      .get
      .container
      .deploy_freeze shouldBe (None)
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
      |      url: http://localhost:7099/singularity
      |""".stripMargin

  def YamlInvalidRandom =
    Source.fromURL(getClass.getResource("/config/invalid-random.yml")).mkString

  def YamlInvalidWithoutVersion =
    Source
      .fromURL(getClass.getResource("/config/invalid-without-version.yml"))
      .mkString

  def YamlInvalidWithIncompleteConf =
    Source
      .fromURL(getClass.getResource("/config/invalid-with-incomplete-conf.yml"))
      .mkString

  def YamlExampleValid =
    Source.fromURL(getClass.getResource("/config/example-config.yml")).mkString

  def YamlExampleExecutorEnvs =
    Source
      .fromURL(getClass.getResource("/config/example-config-with-executor.yml"))
      .mkString

  def YamlExampleAfterDeploy =
    Source
      .fromURL(
        getClass.getResource("/config/example-config-with-after-deploy.yml")
      )
      .mkString

  def YamlJobExampleValid =
    Source
      .fromURL(
        getClass.getResource("/config/example-without-optional-config.yml")
      )
      .mkString

  def YamlExamplePortConfig =
    Source
      .fromURL(getClass.getResource("/config/example-port-config.yml"))
      .mkString

  def YamlExamplePortConfigAllVariations =
    Source
      .fromURL(
        getClass.getResource("/config/example-port-config-all-variations.yml")
      )
      .mkString

  def YamlInvalidPortConfig =
    Source
      .fromURL(getClass.getResource("/config/invalid-port-config.yml"))
      .mkString

  def YamlInvalidPortMapConfig =
    Source
      .fromURL(getClass.getResource("/config/invalid-port-map-config.yml"))
      .mkString

  def YamlMesosAttributeConfig =
    Source
      .fromURL(
        getClass.getResource("/config/example-mesos-attribute-config.yml")
      )
      .mkString

  def YamlExampleWithDeployFreeze =
    Source
      .fromURL(getClass.getResource("/config/example-config-deploy-freeze.yml"))
      .mkString

  def YamlExampleWithoutDeployFreeze =
    Source.fromURL(getClass.getResource("/config/example-config.yml")).mkString

  def YamlEnvVarExampleValid =
    Source
      .fromURL(
        getClass.getResource(
          "/config/example-without-optional-envvar-config.yml"
        )
      )
      .mkString
}
