/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.Keys._

import sbt._
import sbt.plugins.JvmPlugin
import Dependencies._

object Common extends AutoPlugin {

  override def trigger = noTrigger

  override def requires = JvmPlugin

  val repoName = "twirl"

  val javacParameters = Seq(
    "-encoding",
    "UTF-8",
    "-Xlint:-options",
    "--release",
    "11",
    "-Xlint:deprecation",
    "-Xlint:unchecked"
  )

  val scalacParameters = Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-encoding",
    "utf8"
  )

  def crossScalacOptions(version: String) = {
    CrossVersion.partialVersion(version) match {
      case Some((2, n)) if n < 12 =>
        scalacParameters ++ Seq(
          "-release:11",
          "-Ywarn-unused:imports",
          "-Xlint:nullary-unit",
          "-Xlint",
          "-Ywarn-dead-code"
        )
      case _ => scalacParameters
    }
  }

  override def projectSettings =
    Seq(
      scalaVersion       := Scala212,
      crossScalaVersions := ScalaVersions,
      scalacOptions ++= crossScalacOptions(scalaVersion.value),
      javacOptions ++= javacParameters
    )

  override def globalSettings =
    Seq(
      organization         := "com.typesafe.play",
      organizationName     := "Lightbend Inc.",
      organizationHomepage := Some(url("https://www.lightbend.com/")),
      homepage             := Some(url(s"https://github.com/playframework/${repoName}")),
      licenses             := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
      developers += Developer(
        "contributors",
        "Contributors",
        "https://gitter.im/playframework/contributors",
        url("https://github.com/playframework")
      ),
      description := "Twirl"
    )

}
