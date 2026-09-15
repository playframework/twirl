// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = project
  .in(file("."))
  .enablePlugins {
    // Make sure scalajs plugin is not available
    val sjsPluginName = "org.scalajs.sbtplugin.ScalaJSPlugin"
    try Class.forName(sjsPluginName)
    catch {
      case _: ClassNotFoundException => // do nothing
      case _: Throwable => throw new IllegalStateException(s"Found $sjsPluginName, but scalajs should not be required!")
    }
    // Add the twirl plugin
    SbtTwirl
  }
  .settings(
    scalaVersion := {
      val testedVersion = sys.props.getOrElse("scala.version", "3.3.8")
      // This fixture deliberately verifies Scala-3-only behavior (including a template with
      // more than 22 parameters), so retain the canonical Scala 3 baseline in the Scala 2 lane.
      if (testedVersion.startsWith("2.")) "3.3.8" else testedVersion
    },
    scalacOptions ++= (if (scalaVersion.value.startsWith("2.")) Seq("-Xsource:3") else Seq("-source:future")) ++
      Seq("-feature") ++
      (if (scalaVersion.value.startsWith("3.3.")) Seq("-release:17", "-Yfuture-lazy-vals") else Seq.empty)
  )
