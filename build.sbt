import Dependencies._
import sbtcrossproject.crossProject
import org.scalajs.jsenv.nodejs.NodeJSEnv

// Binary compatibility is this version
val previousVersion: Option[String] = Some("1.5.0")

val ScalaTestVersion              = "3.2.8"
val ScalaXmlVersion               = "2.0.0-RC1"
val ScalaParserCombinatorsVersion = "1.2.0-RC2"

val mimaSettings = Seq(
  mimaPreviousArtifacts := previousVersion.map(organization.value %% name.value % _).toSet
)

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
    publish / skip := true
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
  .enablePlugins(Common, Playdoc, Omnidoc, PublishLibrary)
  .configs(Docs)
  .settings(
    mimaSettings,
    name := "twirl-api",
    jsEnv := nodeJs,
    // hack for GraalVM, see: https://github.com/scala-js/scala-js/issues/3673
    // and https://github.com/playframework/twirl/pull/339
    testFrameworks := List(
      new TestFramework(
        "org.scalatest.tools.Framework",
        "org.scalatest.tools.ScalaTestFramework"
      )
    ),
    libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % ScalaXmlVersion,
    libraryDependencies += "org.scalatest"          %%% "scalatest" % ScalaTestVersion % Test,
  )

lazy val apiJvm = api.jvm
lazy val apiJs  = api.js

lazy val parser = project
  .in(file("parser"))
  .enablePlugins(Common, Omnidoc, PublishLibrary)
  .settings(
    mimaSettings,
    name := "twirl-parser",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % ScalaParserCombinatorsVersion % Optional,
    libraryDependencies += "com.novocode"            % "junit-interface"          % "0.11"                        % Test,
    libraryDependencies += "org.scalatest"         %%% "scalatest"                % ScalaTestVersion              % Test,
  )

lazy val compiler = project
  .in(file("compiler"))
  .enablePlugins(Common, Omnidoc, PublishLibrary)
  .dependsOn(apiJvm, parser % "compile;test->test")
  .settings(
    mimaSettings,
    name := "twirl-compiler",
    libraryDependencies +=
      (if (ScalaArtifacts.isScala3(scalaVersion.value))
         "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
       else
         ("org.scala-lang" % "scala-compiler" % scalaVersion.value)
           .exclude("org.scala-lang.modules", s"scala-xml_${scalaBinaryVersion.value}")),
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % ScalaParserCombinatorsVersion % "optional",
    run / fork := true,
  )

lazy val plugin = project
  .in(file("sbt-twirl"))
  .enablePlugins(PublishSbtPlugin, SbtPlugin)
  .dependsOn(compiler)
  .settings(
    name := "sbt-twirl",
    organization := "com.typesafe.sbt",
    scalaVersion := Scala212,
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalaTestVersion % Test,
    Compile / resourceGenerators += generateVersionFile.taskValue,
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
