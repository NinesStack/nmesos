name := "nmesos"
version := scala.io.Source.fromFile("VERSION.txt").getLines().next
maintainer := "roland@tritsch.org"
organization := "nitro"
scalaVersion := "3.0.0-RC1"

// --- generate build info ---
enablePlugins(BuildInfoPlugin)

lazy val buildInfoSettings = Seq(
  buildInfoPackage := "com.nitro.nmesos",
  buildInfoKeys := Seq[BuildInfoKey](version)
)

// --- assembly ---
val execJava = Seq[String](
  "#!/usr/bin/env sh",
  """exec java -Djava.compiler=NONE -noverify -jar "$0" "$@""""
)

lazy val assemblySettings = Seq(
  mainClass in assembly := Some("com.nitro.nmesos.cli.Main"),
  assemblyOption in assembly := (assemblyOption in assembly)
    .value
    .copy(prependShellScript = Some(execJava)
  ),
  assemblyJarName in assembly := "nmesos"
)

// --- sbt-native-packager ---
enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)

lazy val packerSettings = Seq(
  mappings in Universal in packageZipTarball := Seq(
    file("README.md") -> "README.md",
    file((assemblyOutputPath in assembly).value.getPath) -> "nmesos"
  ),
  mappings in (Compile, packageDoc) := Seq()  
)

import com.typesafe.sbt.packager.SettingsHelper._
makeDeploymentSettings(Universal, packageZipTarball, "tgz")

// --- aws-s3-resolver ---
lazy val resolverSettings = Seq(
  awsProfile := Some("nmesos"),
  publishTo := Some(
    s3resolver.value("nmesos-releases", s3("nmesos-releases/nitro-public/repo"))
  ),
  s3overwrite := true
)

// --- build ---
lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-Xfatal-warnings"
  ),
  excludeLintKeys in Global += artifacts in Universal,
  excludeLintKeys in Global += configuration in Universal,
  excludeLintKeys in Global += publishMavenStyle in Universal,
  excludeLintKeys in Global += pushRemoteCacheArtifact in Universal
)

lazy val libsLogging = Seq(
  libraryDependencies += "org.codehaus.janino" % "janino" % "3.1.3",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
  libraryDependencies += "org.log4s" %% "log4s" % "1.10.0-M5"
)

lazy val libsTesting = Seq(
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.5",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.5" % "test"
)

lazy val libs = Seq(
  libraryDependencies += "joda-time" % "joda-time" % "2.10.10",
  libraryDependencies += "net.jcazevedo" % "moultingyaml_2.13" % "0.4.2",
  libraryDependencies += "com.github.scopt" % "scopt_2.13" % "4.0.0",
  libraryDependencies += "org.scalaj" % "scalaj-http_2.13" % "2.4.2",
  libraryDependencies += "com.lihaoyi" %% "upickle" % "1.3.0"
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
