package com.nitro.nmesos.sbt

import com.nitro.nmesos.sbt.cli.ArgumentsParser
import com.nitro.nmesos.sbt.model.ReleaseArgs
import sbt.Keys._
import sbt._
import sbt.complete.Parsers.spaceDelimited

/**
  * Auto Sbt Plugin for NMesos.
  */
object NmesosPlugin extends AutoPlugin {

  override def trigger = allRequirements

  /**
    * Public task and keys offered by the plugin.
    */
  object autoImport {

    lazy val nmesosRepositoryConfig = taskKey[String](
      """Return the folder with the project deployment configuration.
        | If present, it uses NMESOS_CONFIG_REPOSITORY env, otherwise assumes baseDirectory.
      """.stripMargin
    )

    lazy val nmesosRelease = inputKey[Unit](
      """Release a new version of this service"""
    )
  }

  import autoImport._

  object defaults {

    /**
      * Default value of nmesosRepositoryConfig = env var NMESOS_CONFIG_REPOSITORY
      */
    def nmesosRepositoryConfigKey =
      nmesosRepositoryConfig := {
        val base = baseDirectory.value
        val log = (streams in nmesosRelease).value.log
        NmesosPluginImpl.resolveRepositoryPath(base, log)
      }

    def nmesosReleaseKey =
      nmesosRelease := {
        val args = spaceDelimited(ArgumentsParser.releaseExampleUsage).parsed
        ArgumentsParser.parseReleaseArgs(args) match {
          case None =>
            sys.error(s"Invalid args. ${ArgumentsParser.releaseExampleUsage}")

          case Some(arguments) =>
            val serviceName = name.value
            val log = (streams in nmesosRelease).value.log
            val repositoryConfigPath = file(nmesosRepositoryConfig.value)
            val defaultVersion = sbt.Keys.version.value

            NmesosPluginImpl.release(
              arguments,
              serviceName,
              repositoryConfigPath,
              defaultVersion,
              log
            )

        }
      }

    lazy val defaultSettings: Seq[Setting[_]] =
      Seq(nmesosReleaseKey, nmesosRepositoryConfigKey, nmesosReleaseKey)

  }

  override def projectSettings: Seq[Def.Setting[_]] = defaults.defaultSettings

}
