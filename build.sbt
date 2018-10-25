import interplay.ScalaVersions._
import sbtcrossproject.crossProject

val commonSettings = Seq(
  scalaVersion := scala210,
  crossScalaVersions := Seq(scalaVersion.value, scala211, scala212, "2.13.0-M3", "2.13.0-M4", "2.13.0-M5")
)

lazy val twirl = project
    .in(file("."))
    .enablePlugins(PlayRootProject)
    .enablePlugins(CrossPerProjectPlugin)
    .settings(commonSettings: _*)
    .settings(releaseCrossBuild := false)
    .aggregate(apiJvm, apiJs, parser, compiler, plugin)

lazy val api = crossProject(JVMPlatform, JSPlatform)
    .in(file("api"))
    .enablePlugins(PlayLibrary, Playdoc)
    .settings(commonSettings: _*)
    .settings(
      name := "twirl-api",
      libraryDependencies ++= scalaXml.value,
      libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest(scalaVersion.value) % "test"
    )

lazy val apiJvm = api.jvm
lazy val apiJs = api.js

lazy val parser = project
    .in(file("parser"))
    .enablePlugins(PlayLibrary)
    .settings(commonSettings: _*)
    .settings(
      name := "twirl-parser",
      libraryDependencies ++= scalaParserCombinators(scalaVersion.value),
      libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
      libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest(scalaVersion.value) % "test"
    )

lazy val compiler = project
    .in(file("compiler"))
    .enablePlugins(PlayLibrary)
    .dependsOn(apiJvm, parser % "compile;test->test")
    .settings(commonSettings: _*)
    .settings(
      name := "twirl-compiler",
      libraryDependencies += scalaCompiler(scalaVersion.value),
      libraryDependencies ++= scalaParserCombinators(scalaVersion.value),
      fork in run := true
    )

lazy val plugin = project
    .in(file("sbt-twirl"))
    .enablePlugins(PlaySbtPlugin)
    .dependsOn(compiler)
    .settings(
      name := "sbt-twirl",
      organization := "com.typesafe.sbt",
      libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest(scalaVersion.value) % "test",
      resourceGenerators in Compile += generateVersionFile.taskValue,
      scriptedDependencies := {
        scriptedDependencies.value
        publishLocal.all(ScopeFilter(
          inDependencies(compiler)
        )).value
      }
    )

playBuildRepoName in ThisBuild := "twirl"
playBuildExtraTests := {
  (scripted in plugin).toTask("").value
}
playBuildExtraPublish := {
  (PgpKeys.publishSigned in plugin).value
}

// Version file

def generateVersionFile = Def.task {
  val version = (Keys.version in apiJvm).value
  val file = (resourceManaged in Compile).value / "twirl.version.properties"
  val content = s"twirl.api.version=$version"
  IO.write(file, content)
  Seq(file)
}

// Dependencies

def scalatest(scalaV: String): String = scalaV match {
  case "2.13.0-M3" => "3.0.5-M1"
  case "2.13.0-M4" => "3.0.6-SNAP2"
  case _ => "3.0.6-SNAP4"
}

def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version

def scalaParserCombinators(scalaVersion: String): Seq[ModuleID] = scalaVersion match {
  case interplay.ScalaVersions.scala210 => Seq.empty
  case "2.13.0-M3" => Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0" % "optional"
  )
  case _ => Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1" % "optional"
  )
}

def scalaXml = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((x, y)) if x > 2 || (x == 2 && y >= 11) =>
      Seq("org.scala-lang.modules" %%% "scala-xml" % "1.1.0")
    case _ =>
      Seq.empty
  }
}
