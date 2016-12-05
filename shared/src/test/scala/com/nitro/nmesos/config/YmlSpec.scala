package com.nitro.nmesos.config

import com.nitro.nmesos.util.InfoLogger
import com.nitro.nmesos.config.YamlParser._
import com.nitro.nmesos.util.VerboseLogger
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
      YamlParser.parse(YamlExampleValid, VerboseLogger) should beAnInstanceOf[ValidYaml]
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

  val YamlExampleValid = Source.fromURL(getClass.getResource("/config/example-config.yml")).mkString

}