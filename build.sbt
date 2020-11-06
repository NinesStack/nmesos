name in ThisBuild := "nmesos"
organization in ThisBuild := "com.gonitro"
version in ThisBuild := "0.2.21"
scalaVersion in ThisBuild := "2.12.10"

scalacOptions in ThisBuild ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

lazy val cli = Project("nmesos-cli", file("cli"))
  .dependsOn(shared)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)

lazy val shared = Project("nmesos-shared", file("shared"))
  .enablePlugins(BuildInfoPlugin)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    buildInfoPackage := "com.nitro.nmesos",
    crossScalaVersions := Seq("2.12.10", "2.12.1", "2.10.6"),
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.3.1",
      "com.lihaoyi" %% "upickle" % "0.4.4",
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.specs2" %% "specs2-core" % "3.8.6" % "it,test"
    ))

lazy val root =
  project.in(file("."))
    .dependsOn(cli)

import scala.sys.process._
lazy val updateBrew = taskKey[Unit]("Update the brew formula")
updateBrew := {
  s"./Formula/update.sh ${version.value}" !
}
