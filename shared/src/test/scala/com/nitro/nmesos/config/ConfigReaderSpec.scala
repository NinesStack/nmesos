package com.nitro.nmesos.config

import com.nitro.nmesos.config.YamlParser.ValidYaml
import com.nitro.nmesos.util.InfoLogger
import org.specs2.mutable.Specification

import scala.io.Source

class ConfigReaderSpec extends Specification with YmlTestFixtures2 {
  "Config Reader" should {
    "return a Map of all environments with missing env_var keys" in {
      val expectedMissingKeys = Map(
        "dev" -> Set("only_prod", "only_test"),
        "prod" -> Set("only_dev", "only_test"),
        "test" -> Set("only_dev", "only_prod", "dev_and_prod"))
      val parsed = YamlParser.parse(YamlExampleWithMissingEnvVars, InfoLogger).asInstanceOf[ValidYaml]
      val missingEnvVarKeys = ConfigReader.findMissingContainerEnvVarKeys(parsed.config)
      missingEnvVarKeys should be equalTo expectedMissingKeys
    }

    "return an empty map when there are no missing env_var keys" in {
      val parsed = YamlParser.parse(YamlExampleWithValidEnvVars, InfoLogger).asInstanceOf[ValidYaml]
      val missingEnvVarKeys = ConfigReader.findMissingContainerEnvVarKeys(parsed.config)
      missingEnvVarKeys should be equalTo Map()
    }
  }

}

trait YmlTestFixtures2 {
  def YamlExampleWithMissingEnvVars = Source.fromURL(getClass.getResource("/config/example-config-with-missing-feature-toggles.yml")).mkString

  def YamlExampleWithValidEnvVars = Source.fromURL(getClass.getResource("/config/example-config-with-feature-toggles-for-one-environment.yml")).mkString
}
