import Dependencies._
import sbtcrossproject.crossProject
import org.scalajs.jsenv.nodejs.NodeJSEnv

// Binary compatibility is this version
val previousVersion: Option[String] = Some("1.5.0")

val ScalaTestVersion              = "3.1.4"
val ScalaXmlVersion               = "1.3.0"
val ScalaParserCombinatorsVersion = "1.1.2"

val mimaSettings = Seq(
  mimaPreviousArtifacts := previousVersion.map(organization.value %% name.value % _).toSet
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
dynverTagPrefix in ThisBuild := ""

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
    libraryDependencies += "org.scalatest"          %%% "scalatest" % ScalaTestVersion % "test",
  )

lazy val apiJvm = api.jvm
lazy val apiJs  = api.js

lazy val parser = project
  .in(file("parser"))
  .enablePlugins(Common, Omnidoc, PublishLibrary)
  .settings(
    mimaSettings,
    name := "twirl-parser",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % ScalaParserCombinatorsVersion % "optional",
    libraryDependencies += "com.novocode"            % "junit-interface"          % "0.11"                        % "test",
    libraryDependencies += "org.scalatest"         %%% "scalatest"                % ScalaTestVersion              % "test",
  )

lazy val compiler = project
  .in(file("compiler"))
  .enablePlugins(Common, Omnidoc, PublishLibrary)
  .dependsOn(apiJvm, parser % "compile;test->test")
  .settings(
    mimaSettings,
    name := "twirl-compiler",
    libraryDependencies += "org.scala-lang"          % "scala-compiler"           % scalaVersion.value,
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % ScalaParserCombinatorsVersion % "optional",
    fork in run := true,
  )

lazy val plugin = project
  .in(file("sbt-twirl"))
  .enablePlugins(PublishSbtPlugin, SbtPlugin)
  .dependsOn(compiler)
  .settings(
    name := "sbt-twirl",
    organization := "com.typesafe.sbt",
    scalaVersion := Scala212,
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalaTestVersion % "test",
    resourceGenerators in Compile += generateVersionFile.taskValue,
    scriptedDependencies := {
      scriptedDependencies.value
      publishLocal
        .all(
          ScopeFilter(
            inDependencies(compiler)
          )
        )
        .value
    },
    mimaFailOnNoPrevious := false,
  )

// Version file
def generateVersionFile =
  Def.task {
    val version = (Keys.version in apiJvm).value
    val file    = (resourceManaged in Compile).value / "twirl.version.properties"
    val content = s"twirl.api.version=$version"
    IO.write(file, content)
    Seq(file)
  }

addCommandAlias("validateCode", ";headerCheckAll;+scalafmtCheckAll;scalafmtSbtCheck")
