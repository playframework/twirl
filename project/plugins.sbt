addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.get("interplay.version").getOrElse("2.1.10"))

// For the Cross Build
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.11.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.6.4")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.3.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"    % "2.2.1")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")
