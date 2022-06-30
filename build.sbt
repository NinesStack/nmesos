name := "nmesos"
version := scala.io.Source.fromFile("VERSION.txt").getLines().next
maintainer := "roland@tritsch.email"
organization := "ninesstack"
scalaVersion := "3.2.0-RC1"

// --- add task to update the asdf versions ---
import scala.sys.process._
lazy val updateAsdf = taskKey[Unit]("Update the asdf versions")
updateAsdf := {
  val log = streams.value.log
  s"./bin/updateAsdfSbt.sh ${version.value}" ! log
}

// --- add task to update the brew formula ---
import scala.sys.process._
lazy val updateBrew = taskKey[Unit]("Update the brew formula")
updateBrew := {
  val log = streams.value.log
  s"./Formula/updateBrewSbt.sh ${version.value}" ! log
}

// --- generate build info ---
enablePlugins(BuildInfoPlugin)

lazy val buildInfoSettings = Seq(
  buildInfoPackage := "ninesstack.nmesos",
  buildInfoKeys := Seq[BuildInfoKey](version)
)

// --- assembly ---
val execJava = Seq[String](
  "#!/usr/bin/env sh",
  """exec java -Djava.compiler=NONE -noverify -jar "$0" "$@""""
)

lazy val assemblySettings = Seq(
  assembly / mainClass := Some("ninesstack.nmesos.cli.Main"),
  assembly / assemblyPrependShellScript := Some(execJava),
  assembly / assemblyJarName := "nmesos"
)

// --- sbt-native-packager ---
enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)

lazy val packerSettings = Seq(
  Universal / packageZipTarball / mappings := Seq(
    file("README.md") -> "README.md",
    file((assembly / assemblyOutputPath).value.getPath) -> "nmesos"
  ),
  Compile / packageDoc / mappings := Seq()  
)

import com.typesafe.sbt.packager.SettingsHelper._
makeDeploymentSettings(Universal, packageZipTarball, "tgz")

// --- aws-s3-resolver ---
lazy val resolverSettings = Seq(
  publishMavenStyle := true,
  publishTo := Some("nmesos-releases" at "s3://s3-eu-west-1.amazonaws.com/nmesos-releases/public")
)

// --- build ---
lazy val commonSettings = Seq(
  versionScheme := Some("semver-spec"),
  scalacOptions ++= Seq(
    "-Xfatal-warnings"
  ),

  Global / excludeLintKeys += Universal / artifacts,
  Global / excludeLintKeys += Universal / configuration,
  Global / excludeLintKeys += Universal / publishMavenStyle,
  Global / excludeLintKeys += Universal / pushRemoteCacheArtifact
)

lazy val libsLogging = Seq(
  libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.17.2",
  libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.17.2"
)

lazy val libsTesting = Seq(
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.12",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % "test"
)

lazy val libs = Seq(
  libraryDependencies += "joda-time" % "joda-time" % "2.10.14",
  libraryDependencies += "net.jcazevedo" % "moultingyaml_2.13" % "0.4.2",
  libraryDependencies += "com.github.scopt" % "scopt_2.13" % "4.0.0",
  libraryDependencies += "org.scalaj" % "scalaj-http_2.13" % "2.4.2",
  libraryDependencies += "com.lihaoyi" %% "upickle" % "2.0.0"
)

lazy val root = project
  .in(file("."))
  .settings(buildInfoSettings)
  .settings(assemblySettings)
  .settings(resolverSettings)
  .settings(packerSettings)
  .settings(commonSettings)
  .settings(libsLogging)
  .settings(libsTesting)
  .settings(libs)
