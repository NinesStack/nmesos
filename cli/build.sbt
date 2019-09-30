// Assembly as a CLI auto runnable bin.

scalaVersion := "2.12.1"
organization := "nitro"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "org.specs2" %% "specs2-core" % "3.8.6" % "it,test"
)

val cli = Seq[String](
  "#!/usr/bin/env sh",
  """exec java -Djava.compiler=NONE -noverify -jar "$0" "$@""""
)

mainClass in assembly := Some("com.nitro.nmesos.cli.Main")
assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(cli))
assemblyJarName in assembly := "nmesos"



enablePlugins(UniversalPlugin)
packageName in Universal := "nmesos-cli-" + version.value
mappings in Universal in packageZipTarball := {
  Seq(
    file("README.md") -> "README.md",
    file("contrib/etc/bash_completion.d/nmesos") -> "contrib/etc/bash_completion.d/nmesos",
    file((assemblyOutputPath in assembly).value.getPath) -> "nmesos"
  )
}

import com.typesafe.sbt.packager.SettingsHelper._
makeDeploymentSettings(Universal, packageZipTarball, "tgz")

publishTo := {
  Some("S3" at "s3://s3.amazonaws.com/nitro-public/repo")
}
