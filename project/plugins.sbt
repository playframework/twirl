addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.get("interplay.version").getOrElse("1.2.0"))

// For the Cross Build
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.11")