logLevel := Level.Debug

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin, SbtTwirl)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    mainClass                       := Some("Test")
  )
