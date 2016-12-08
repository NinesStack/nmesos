// Assembly as a CLI auto runnable bin.

scalaVersion := "2.12.0"
organization := "nitro"

val cli = Seq(
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
    file((assemblyOutputPath in assembly).value.getPath) -> "nmesos"
  )
}

import com.typesafe.sbt.packager.SettingsHelper._
makeDeploymentSettings(Universal, packageZipTarball, "tgz")

publishTo := {
  Some("S3" at "s3://s3.amazonaws.com/nitro-public/repo")
}
