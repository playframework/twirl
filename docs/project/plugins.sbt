lazy val plugins = (project in file(".")).dependsOn(sbtTwirl)

lazy val sbtTwirl = ProjectRef(Path.fileProperty("user.dir").getParentFile, "plugin")

// use special snapshot play version for now

resolvers ++= DefaultOptions.resolvers(snapshot = true)

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.typesafe.play" % "play-docs-sbt-plugin" % "2.4-2014-10-31-e8009b2-SNAPSHOT")
