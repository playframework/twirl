addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.get("interplay.version").getOrElse("2.1.1"))

// For the Cross Build
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.29")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")

// remove this resolved when https://github.com/lightbend/mima/issues/422 is solved
resolvers += Resolver.url(
  "typesafe sbt-plugins",
  url("https://dl.bintray.com/typesafe/sbt-plugins")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.6.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.2.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"    % "2.0.7")
