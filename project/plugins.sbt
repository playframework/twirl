addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.get("interplay.version").getOrElse("2.1.6"))

def buildForSbt013 = System.getProperty("sbt013", "").trim.equals("true")

// For the Cross Build
addSbtPlugin("org.scala-js" % "sbt-scalajs" % (buildForSbt013 match {
  case true => "0.6.33"
  case _    => "1.12.0"
}))

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % (buildForSbt013 match {
  case true => "0.6.1"
  case _    => "1.2.0"
}))

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.7.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.3.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"    % "2.2.1")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")
