name in ThisBuild := "nmesos"
organization in ThisBuild := "com.nitro.nmesos"
version in ThisBuild := "0.1.0"
scalaVersion in ThisBuild := "2.11.8"


lazy val cli = Project("cli", file("cli"))
  .dependsOn(shared)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.5.0",
      "org.specs2" %% "specs2-core" % "3.8.5" % "test"
    ))

lazy val shared = Project("shared", file("shared"))
  .enablePlugins(BuildInfoPlugin)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    buildInfoPackage := "com.nitro.nmesos",
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.3.1",
      "com.lihaoyi" %% "upickle" % "0.4.3",
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.specs2" %% "specs2-core" % "3.8.5" % "it,test"
    ))