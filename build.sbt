val scala3Version = "3.0.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "nmesos",
    version := "1.0.0",
    scalaVersion := scala3Version,
    libraryDependencies += "de.sciss" %% "log" % "0.1.1",
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.5",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.5" % "test"
  )
