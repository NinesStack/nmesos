package ninesstack.nmesos.config

import java.io.{File, FileNotFoundException}

import ninesstack.nmesos.BuildInfo
import ninesstack.nmesos.util.{Formatter, VersionUtil}
import ninesstack.nmesos.config.model._
import ninesstack.nmesos.config.YamlParser._
import ninesstack.nmesos.util.VersionUtil.Version

import scala.io.Source
import scala.util.{Failure, Success, Try}

object ConfigReader {
  private val logger = org.apache.log4j.Logger.getLogger(this.getClass.getName)

  sealed trait ConfigResult

  case class ConfigError(msg: String, yamlFile: File) extends ConfigResult
  case class ValidConfig(
      environment: Environment,
      environmentName: String,
      fileHash: String,
      file: File
  ) extends ConfigResult

  /**
    * Read all the  Yaml configuration for an environment.
    */
  def parseEnvironment(
      file: File,
      environmentName: String,
      fmt: Formatter
  ): ConfigResult = {
    parse(file, fmt) match {
      case InvalidYaml(msg) =>
        ConfigError(msg, file)

      case ValidYaml(config, hash) =>
        val envVarDifferences = findMissingContainerEnvVarKeys(config)
        if (envVarDifferences.nonEmpty) {
          fmt.fmtBlock("Environment env_var keys not equal") {
            for (diff <- envVarDifferences) {
              fmt.error(
                s"Environment ${diff._1}: Missing env_var keys: ${diff._2.mkString(", ")}"
              )
            }
          }
        }
        environmentFromConfig(config, environmentName, hash, file)

    }
  }

  /**
    * Check all feature-toggles (env_var keys) of every environment and
    * return a Map[EnvironmentName, Set[String]] of missing env_var keys.
    * key: environment name
    * val: a Set of all keys that are missing in this environment
    */
  def findMissingContainerEnvVarKeys(
      config: Config
  ): Map[EnvironmentName, Set[String]] = {
    val allEnvVarKeys = config.environments.foldLeft(Set[String]()) {
      case (allKeys, (_, environment)) =>
        environment.container.env_vars match {
          case Some(envVars) => envVars.keys.toSet ++ allKeys
          case None          => allKeys
        }
    }

    (for {
      (envName, environment) <- config.environments
      envVars <- environment.container.env_vars
    } yield {
      val missingKeys = allEnvVarKeys.diff(envVars.keys.toSet)
      if (missingKeys.isEmpty)
        None
      else
        Some(envName, missingKeys)
    }).flatten.toMap
  }

  /**
    *  Try to read a Yaml file.
    */
  private def parse(file: File, fmt: Formatter): ParserResult = {
    logger.info(s"Reading file ${file.getAbsolutePath}")
    tryRead(file) match {
      case Failure(ex: FileNotFoundException) =>
        InvalidYaml(s"Config file not found at '${file.getAbsoluteFile}'")

      case Failure(ex) =>
        InvalidYaml(
          s"Unexpected error reading file '${file.getAbsoluteFile}'. ${ex.getMessage}"
        )

      case Success((yamlContent, version))
          if (!VersionUtil.isCompatible(version, fmt)) =>
        InvalidYaml(msg = s"""
             |A newer version of nmesos is required.
             |Installed: ${BuildInfo.version}, required: ${version
          .mkString(".")}
             |Instructions at https://github.ninesstack/nmesos/blob/master/README.md
          """.stripMargin)
      case Success((yamlContent, version)) =>
        YamlParser.parse(yamlContent, fmt)
    }
  }

  /**
    * Extract the environment config to use if present.
    */
  private def environmentFromConfig(
      config: Config,
      environmentName: String,
      hash: String,
      file: File
  ): ConfigResult =
    config.environments.get(environmentName) match {
      case None =>
        ConfigError(s"Environment '$environmentName' not found.", file)

      case Some(environment) =>
        ValidConfig(environment, environmentName, hash, file)

    }

  // wrap unsafe read file and version
  private def tryRead(file: File): Try[(String, Version)] =
    for {
      yamlContent <- Try(Source.fromFile(file).mkString)
      requiredVersion <- VersionUtil.tryExtractFromYaml(yamlContent)
    } yield (yamlContent, requiredVersion)

}
