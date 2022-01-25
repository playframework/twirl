import Dependencies._

import sbtcrossproject.crossProject
import org.scalajs.jsenv.nodejs.NodeJSEnv

// Binary compatibility is this version
val previousVersion: Option[String] = Some("1.5.0")

val ScalaTestVersion = "3.2.10"

def parserCombinators(scalaVersion: String) = "org.scala-lang.modules" %% "scala-parser-combinators" % {
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => "1.1.2"
    case _            => "2.1.0"
  }
}

val mimaSettings = Seq(
  mimaPreviousArtifacts := previousVersion.map(organization.value %% name.value % _).toSet
)

ThisBuild / sonatypeProfileName := "com.typesafe.play"

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverTagPrefix := ""

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  if (dynverGitDescribeOutput.value.hasNoTags)
    throw new MessageOnlyException(
      s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: $v"
    )
  s
}

lazy val twirl = project
  .in(file("."))
  .disablePlugins(MimaPlugin)
  .settings(
    crossScalaVersions := Nil, // workaround so + uses project-defined variants
    publish / skip     := true
  )
  .aggregate(apiJvm, apiJs, parser, compiler, plugin)

lazy val nodeJs = {
  if (System.getProperty("NODE_PATH") != null)
    new NodeJSEnv(NodeJSEnv.Config().withExecutable(System.getProperty("NODE_PATH")))
  else
    new NodeJSEnv()
}

lazy val api = crossProject(JVMPlatform, JSPlatform)
  .in(file("api"))
  .enablePlugins(Common, Playdoc, Omnidoc)
  .configs(Docs)
  .settings(
    mimaSettings,
    name  := "twirl-api",
    jsEnv := nodeJs,
    // hack for GraalVM, see: https://github.com/scala-js/scala-js/issues/3673
    // and https://github.com/playframework/twirl/pull/339
    testFrameworks := List(
      new TestFramework(
        "org.scalatest.tools.Framework",
        "org.scalatest.tools.ScalaTestFramework"
      )
    ),
    libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) => "1.3.0"
        case _            => "2.0.1"
      }
    },
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalaTestVersion % Test,
  )

lazy val apiJvm = api.jvm
lazy val apiJs  = api.js

lazy val parser = project
  .in(file("parser"))
  .enablePlugins(Common, Omnidoc)
  .settings(
    mimaSettings,
    name                                                        := "twirl-parser",
    libraryDependencies += parserCombinators(scalaVersion.value) % Optional,
    libraryDependencies += "com.github.sbt"                      % "junit-interface" % "0.13.3"         % Test,
    libraryDependencies += "org.scalatest"                     %%% "scalatest"       % ScalaTestVersion % Test,
  )

lazy val compiler = project
  .in(file("compiler"))
  .enablePlugins(Common, Omnidoc)
  .dependsOn(apiJvm, parser % "compile;test->test")
  .settings(
    mimaSettings,
    name                                                        := "twirl-compiler",
    libraryDependencies += scalaVersion { v =>
      CrossVersion.partialVersion(v) match {
        case Some((3, _)) => "org.scala-lang" %% "scala3-compiler" % v
        case _            => "org.scala-lang" %  "scala-compiler"  % v
      }
    }.value,
    libraryDependencies += parserCombinators(scalaVersion.value) % "optional",
    libraryDependencies += "org.scalameta"                       %% "scalameta"      % "4.4.33" cross CrossVersion.for3Use2_13,
    run / fork                                                  := true,
  )

lazy val plugin = project
  .in(file("sbt-twirl"))
  .enablePlugins(SbtPlugin)
  .dependsOn(compiler)
  .settings(
    name                                    := "sbt-twirl",
    organization                            := "com.typesafe.play",
    scalaVersion                            := Scala212,
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalaTestVersion % Test,
    Compile / resourceGenerators += generateVersionFile.taskValue,
    scriptedLaunchOpts += version.apply { v => s"-Dproject.version=$v" }.value,
    // both `locally`s are to work around sbt/sbt#6161
    scriptedDependencies := {
      locally { val _ = scriptedDependencies.value }
      locally {
        val _ = publishLocal
          .all(
            ScopeFilter(
              inAnyProject
            )
          )
          .value
      }
      ()
    },
    mimaFailOnNoPrevious := false,
  )

// Version file
def generateVersionFile =
  Def.task {
    val version = (apiJvm / Keys.version).value
    val file    = (Compile / resourceManaged).value / "twirl.version.properties"
    val content = s"twirl.api.version=$version"
    IO.write(file, content)
    Seq(file)
  }

addCommandAlias("validateCode", ";headerCheckAll;+scalafmtCheckAll;scalafmtSbtCheck")
