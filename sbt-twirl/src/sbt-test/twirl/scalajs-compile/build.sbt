
logLevel := Level.Debug

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin, SbtTwirl)
  .settings(
    libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "1.3.0",
    scalaJSUseMainModuleInitializer := true,
    mainClass := Some("Test")
  )
