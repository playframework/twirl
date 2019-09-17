addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.get("interplay.version").getOrElse("2.0.9"))

// For the Cross Build
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.29")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.6.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.2.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"    % "2.0.5")
