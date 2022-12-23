addSbtPlugin("com.typesafe.play" % "sbt-twirl" % sys.props("project.version"))

def buildForSbt013 = System.getProperty("sbt013", "").trim.equals("true")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % (buildForSbt013 match {
  case true => "0.6.33"
  case _    => "1.12.0"
}))
