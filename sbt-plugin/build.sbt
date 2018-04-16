sbtPlugin := true

name := "sbt-nmesos"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "com.nitro.nmesos" %% "nmesos-shared" % "0.2.0",
  "org.specs2" %% "specs2-core" % "3.8.6" % "test"
)
