name in ThisBuild := "nmesos"
organization in ThisBuild := "com.nitro"
version in ThisBuild := "0.1-SNAPSHOT"

crossScalaVersions in ThisBuild := Seq("2.12.0", "2.10.6")

lazy val cli = Project("cli", file("cli"))
  .dependsOn(shared)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.5.0",
      "org.specs2" %% "specs2-core" % "3.8.6" % "test"
    ))

lazy val sbtPlugin = Project("sbt-plugin", file("sbt-plugin"))
  .dependsOn(shared)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.5.0",
      "org.specs2" %% "specs2-core" % "3.8.6" % "test"
    ))

lazy val shared = Project("shared", file("shared"))
  .enablePlugins(BuildInfoPlugin)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    scalaVersion := "2.12.0",
    crossScalaVersions := Seq("2.12.0", "2.10.6"),
    buildInfoPackage := "com.nitro.nmesos",
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.3.1",
      "com.lihaoyi" %% "upickle" % "0.4.4",
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.specs2" %% "specs2-core" % "3.8.6" % "it,test"
    ))