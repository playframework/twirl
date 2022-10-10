lazy val plugins = (project in file(".")).dependsOn(sbtTwirl).settings(
  excludeDependencies += "com.typesafe.sbt" % "sbt-native-packager", // TODO: remove when Play switched to com.github.sbt
  scalaVersion := "2.12.17", // TODO: remove when upgraded to sbt 1.8.0 (maybe even 1.7.2), see https://github.com/sbt/sbt/pull/7021
)

lazy val sbtTwirl = ProjectRef(Path.fileProperty("user.dir").getParentFile, "plugin")

resolvers ++= DefaultOptions.resolvers(snapshot = true)

addSbtPlugin("com.typesafe.play" % "play-docs-sbt-plugin" % sys.props.getOrElse("play.version", "2.9.0-M2"))
addSbtPlugin("de.heikoseeberger" % "sbt-header"           % "5.7.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"         % "2.4.6")
addSbtPlugin("com.github.sbt"    % "sbt-native-packager"  % "1.9.11") // TODO: remove when Play itself pulls in 1.9.11+
