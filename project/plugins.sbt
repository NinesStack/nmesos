addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.8.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

resolvers += Resolver.jcenterRepo
addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.19.0")
