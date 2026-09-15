// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

logLevel := Level.Debug

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin, SbtTwirl)
  .settings(
    scalaVersion                     := sys.props("scala.version"),
    scalaJSUseMainModuleInitializer := true,
    mainClass                       := Some("Test")
  )
