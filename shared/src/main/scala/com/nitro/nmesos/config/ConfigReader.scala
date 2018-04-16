package com.nitro.nmesos.config

import java.io.{ File, FileNotFoundException }

import com.nitro.nmesos.BuildInfo
import com.nitro.nmesos.util.{ Logger, VersionUtil }
import com.nitro.nmesos.config.model._
import com.nitro.nmesos.config.YamlParser._
import com.nitro.nmesos.util.VersionUtil.Version

import scala.io.Source
import scala.util.{ Failure, Success, Try }

object ConfigReader {

  sealed trait ConfigResult

  case class ConfigError(msg: String, yamlFile: File) extends ConfigResult
  case class ValidConfig(
    environment: Environment,
    environmentName: String,
    fileHash: String,
    file: File) extends ConfigResult

  /**
   * Read all the  Yaml configuration for an environment.
   */
  def parseEnvironment(file: File, environmentName: String, logger: Logger): ConfigResult = {
    parse(file, logger) match {
      case InvalidYaml(msg) =>
        ConfigError(msg, file)

      case ValidYaml(config, hash) =>
        environmentFromConfig(config, environmentName, hash, file)

    }
  }

  /**
   *  Try to read a Yaml file.
   */
  private def parse(file: File, logger: Logger): ParserResult = {
    logger.debug(s"Reading file ${file.getAbsolutePath}")
    tryRead(file) match {
      case Failure(ex: FileNotFoundException) =>
        InvalidYaml(s"Config file not found at '${file.getAbsoluteFile}'")

      case Failure(ex) =>
        InvalidYaml(s"Unexpected error reading file '${file.getAbsoluteFile}'. ${ex.getMessage}")

      case Success((yamlContent, version)) if (!VersionUtil.isCompatible(version, logger)) =>
        InvalidYaml(msg =
          s"""
             |A newer version of nmesos is required.
             |Installed: ${BuildInfo.version}, required: ${version.mkString(".")}
             |Instructions at https://github.com/Nitro/nmesos/blob/master/README.md
          """.stripMargin)
      case Success((yamlContent, version)) =>
        YamlParser.parse(yamlContent, logger)
    }
  }

  /**
   * Extract the environment config to use if present.
   */
  private def environmentFromConfig(config: Config, environmentName: String, hash: String, file: File): ConfigResult =
    config.environments.get(environmentName) match {
      case None =>
        ConfigError(s"Environment '$environmentName' not found.", file)

      case Some(environment) =>
        ValidConfig(environment, environmentName, hash, file)

    }

  // wrap unsafe read file and version
  private def tryRead(file: File): Try[(String, Version)] = for {
    yamlContent <- Try(Source.fromFile(file).mkString)
    requiredVersion <- VersionUtil.tryExtractFromYaml(yamlContent)
  } yield (yamlContent, requiredVersion)

}
