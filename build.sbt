name := "nmesos"
version := "1.0.1"
maintainer := "roland@tritsch.org"
organization := "nitro"
scalaVersion := "3.0.0-RC1"

// --- assembly ---
val execJava = Seq[String](
  "#!/usr/bin/env sh",
  """exec java -Djava.compiler=NONE -noverify -jar "$0" "$@""""
)

mainClass in assembly := Some("com.nitro.nmesos.cli.Main")
assemblyOption in assembly := (assemblyOption in assembly)
  .value
  .copy(prependShellScript = Some(execJava))
assemblyJarName in assembly := "nmesos"

// --- sbt-native-packager ---
enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)

mappings in Universal in packageZipTarball := {
  Seq(
    file("README.md") -> "README.md",
    file((assemblyOutputPath in assembly).value.getPath) -> "nmesos"
  )
}

import com.typesafe.sbt.packager.SettingsHelper._
makeDeploymentSettings(Universal, packageZipTarball, "tgz")

// --- aws-s3-resolver ---
awsProfile := Some("nmesos")
publishTo := Some(
  s3resolver.value("nmesos-releases", s3("nmesos-releases/nitro-public/repo"))
)

// --- build ---
lazy val common = Seq(
  scalacOptions ++= Seq(
    "-Xfatal-warnings"
  )
)

lazy val libs = Seq(
  libraryDependencies += "org.log4s" %% "log4s" % "1.10.0-M5",
  libraryDependencies += "joda-time" % "joda-time" % "2.10.10",
  libraryDependencies += "net.jcazevedo" % "moultingyaml_2.13" % "0.4.2",
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.5",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.5" % "test",
  libraryDependencies += "com.github.scopt" % "scopt_2.13" % "4.0.0",
  libraryDependencies += "org.scalaj" % "scalaj-http_2.13" % "2.4.2",
  libraryDependencies += "com.lihaoyi" %% "upickle" % "1.3.0",
)

lazy val root = project
  .in(file("."))
  .settings(common)
  .settings(libs)
