// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import Dependencies._

import sbtcrossproject.CrossPlugin.autoImport.crossProject
import org.scalajs.jsenv.nodejs.NodeJSEnv
import java.util.Properties
import java.io.StringWriter

val ScalaTestVersion = "3.2.19"

def parserCombinators(scalaVersion: String) = "org.scala-lang.modules" %% "scala-parser-combinators" % {
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => "1.1.2"
    case _            => "2.3.0"
  }
}

val previousVersion: Option[String] = Some("2.0.1")

val mimaSettings = Seq(
  mimaPreviousArtifacts := previousVersion.map(organization.value %% moduleName.value % _).toSet,
  mimaBinaryIssueFilters ++= Seq(
  )
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

lazy val twirl = project
  .in(file("."))
  .disablePlugins(MimaPlugin)
  .settings(
    crossScalaVersions  := Nil, // workaround so + uses project-defined variants
    publish / skip      := true,
    (Compile / headerSources) ++=
      ((baseDirectory.value ** ("*.properties" || "*.md" || "*.sbt" || "*.scala.html"))
        --- (baseDirectory.value ** "target" ** "*")
        --- (baseDirectory.value / "compiler" / "version.properties")
        --- (baseDirectory.value ** "gradle-twirl" ** "*") // Gradle Spotless plugin is used
        --- (baseDirectory.value / "docs" ** "*")).get ++
        (baseDirectory.value / "project" ** "*.scala" --- (baseDirectory.value ** "target" ** "*")).get
  )
  .aggregate(apiJvm, apiJs, parser, compiler, plugin, mavenPlugin)

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
    scalaVersion        := Scala212,
    crossScalaVersions  := ScalaVersions,
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
    libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "2.2.0",
    libraryDependencies += "org.scalatest"          %%% "scalatest" % ScalaTestVersion % Test,
  )

lazy val apiJvm = api.jvm
lazy val apiJs  = api.js

lazy val parser = project
  .in(file("parser"))
  .enablePlugins(Common, Omnidoc)
  .settings(
    scalaVersion        := Scala212,
    crossScalaVersions  := ScalaVersions,
    mimaSettings,
    name := "twirl-parser",
    libraryDependencies += parserCombinators(scalaVersion.value),
    libraryDependencies += "com.github.sbt"  % "junit-interface" % "0.13.3"         % Test,
    libraryDependencies += "org.scalatest" %%% "scalatest"       % ScalaTestVersion % Test,
  )

lazy val compiler = project
  .in(file("compiler"))
  .enablePlugins(Common, Omnidoc, BuildInfoPlugin)
  .settings(
    scalaVersion        := Scala212,
    crossScalaVersions  := ScalaVersions,
    mimaSettings,
    name := "twirl-compiler",
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) =>
          // only for scala < 3
          Seq("org.scala-lang" % "scala-compiler" % scalaVersion.value % Test)
        case _ => Seq("org.scala-lang" %% "scala3-compiler" % scalaVersion.value % Test)
      }
    },
    libraryDependencies += parserCombinators(scalaVersion.value),
    libraryDependencies += "org.scalameta" %% "parsers" % "4.13.8",
    run / fork                             := true,
    buildInfoKeys                          := Seq[BuildInfoKey](scalaVersion),
    buildInfoPackage                       := "play.twirl.compiler",
    publishM2                              := publishM2.dependsOn(saveCompilerVersion).value,
    publish                                := publish.dependsOn(saveCompilerVersion).value,
    publishLocal                           := publishLocal.dependsOn(saveCompilerVersion).value
  )
  .aggregate(parser)
  .dependsOn(apiJvm % Test, parser % "compile->compile;test->test")

lazy val plugin = project
  .in(file("sbt-twirl"))
  .enablePlugins(SbtPlugin)
  .dependsOn(compiler)
  .settings(
    name                                    := "sbt-twirl",
    organization                            := "org.playframework.twirl",
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
    mimaFailOnNoPrevious := false
  )

lazy val mavenPlugin = project
  .in(file("maven-twirl"))
  .enablePlugins(SbtMavenPlugin)
  .dependsOn(compiler)
  .settings(
    name                  := "twirl-maven-plugin",
    scalaVersion          := Scala212,
    crossScalaVersions    := ScalaVersions,
    mavenPluginGoalPrefix := "twirl",
    mavenLaunchOpts ++= Seq(
      // Uncomment to debug plugin code while Maven scripted test is running
//      "-agentlib:jdwp=transport=dt_socket,address=localhost:62555,suspend=y,server=y",
      version.apply { v => s"-Dplugin.version=$v" }.value,
      scalaVersion.apply { v => s"-Dscala.binaryVersion=${CrossVersion.binaryScalaVersion(v)}" }.value
    ),
    publishM2 := publishM2
      .dependsOn(compiler / publishM2)
      .dependsOn(parser / publishM2)
      .dependsOn(apiJvm / publishM2)
      .value,
    libraryDependencies += "org.codehaus.plexus" % "plexus-utils" % "4.0.2",
    Compile / headerSources ++= (baseDirectory.value / "src" / "maven-test" ** ("*.java" || "*.scala" || "*.scala.html") --- (baseDirectory.value ** "target" ** "*")).get,
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

def saveCompilerVersion =
  Def.task {
    val props  = new Properties()
    val writer = new StringWriter()
    val file   = baseDirectory.value / "version.properties"
    props.setProperty("twirl.compiler.version", version.value)
    props.store(writer, "")
    IO.write(file, writer.getBuffer.toString)
    Seq(file)
  }

addCommandAlias(
  "validateCode",
  List(
    "headerCheckAll",
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
    "javafmtCheckAll",
  ).mkString(";")
)

addCommandAlias(
  "format",
  List(
    "scalafmtSbt",
    "scalafmtAll",
    "javafmtAll",
  ).mkString(";")
)
