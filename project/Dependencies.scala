/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

object Dependencies {
  val scala212Version   = "2.12.21"
  val scala213Version   = "2.13.18"
  val scala33LTSVersion = "3.3.8"
  val scala39LTSVersion = "3.9.0"
  val scala3NextVersion = "3.10.0-RC2"

  val publishedScalaVersions = Seq(scala212Version, scala213Version, scala33LTSVersion)

  private val scalaVersionAliases = Map(
    "2.12.x" -> scala212Version,
    "2.13.x" -> scala213Version,
    "3.3.x"  -> scala33LTSVersion,
    "3.9.x"  -> scala39LTSVersion,
    "3.next" -> scala3NextVersion,
  )

  def resolveScalaVersion(version: String): String = scalaVersionAliases.getOrElse(version, version)
}
