val scala3Version = "3.0.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "nmesos",
    version := "1.0.0",
    scalaVersion := scala3Version,
    libraryDependencies += "de.sciss" %% "log" % "0.1.1",
    libraryDependencies += "joda-time" % "joda-time" % "2.10.10",
    libraryDependencies += "net.jcazevedo" % "moultingyaml_2.13" % "0.4.2",
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.5",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.5" % "test",
    libraryDependencies += "com.github.scopt" % "scopt_2.13" % "4.0.0",
    libraryDependencies += "org.scalaj" % "scalaj-http_2.13" % "2.4.2",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "1.3.0",
  )
