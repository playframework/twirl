
logLevel := Level.Debug

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin, SbtTwirl)
  .settings(
    scalaVersion := sys.props("scala.version"),
    scalaJSUseMainModuleInitializer := true,
    mainClass := Some("Test")
  )
