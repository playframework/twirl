lazy val plugins = (project in file(".")).dependsOn(sbtTwirl)

lazy val sbtTwirl = ProjectRef(Path.fileProperty("user.dir").getParentFile, "plugin")

resolvers ++= DefaultOptions.resolvers(snapshot = true)

addSbtPlugin("com.typesafe.play" % "play-docs-sbt-plugin" % sys.props.getOrElse("play.version", "2.6.23"))
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")
