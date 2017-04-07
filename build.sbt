name in ThisBuild := "nmesos"
organization in ThisBuild := "com.gonitro"
version in ThisBuild := "0.1.2"

lazy val cli = Project("nmesos-cli", file("cli"))
  .dependsOn(shared)
  .settings(
    scalaVersion := "2.12.1",
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.5.0",
      "org.specs2" %% "specs2-core" % "3.8.6" % "test"
    ))

lazy val sbtPlugin = Project("sbt-plugin", file("sbt-plugin"))
  .dependsOn(shared_2_10)
  .settings(
    scalaVersion := "2.10.6",
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.5.0",
      "org.specs2" %% "specs2-core" % "3.8.6" % "test"
    ))

lazy val shared = Project("nmesos-shared", file("shared"))
  .enablePlugins(BuildInfoPlugin)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    buildInfoPackage := "com.nitro.nmesos",
    crossScalaVersions := Seq("2.12.1", "2.10.6"),
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.3.1",
      "com.lihaoyi" %% "upickle" % "0.4.4",
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.specs2" %% "specs2-core" % "3.8.6" % "it,test"
    ))

lazy val shared_2_10 = shared.settings(
  scalaVersion := "2.10.6"
)