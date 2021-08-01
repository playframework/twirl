import Dependencies._
import sbtcrossproject.crossProject
import org.scalajs.jsenv.nodejs.NodeJSEnv

// Binary compatibility is this version
val previousVersion: Option[String] = Some("1.5.0")

// Next line can be removed when dropping Scala 2.12? See https://github.com/playframework/twirl/pull/424
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

val ScalaTestVersion              = "3.2.9"
val ScalaXmlVersion               = "2.0.1"
val ScalaParserCombinatorsVersion = "2.0.0"

// temporarily needed for scaladoc generation Scala 3.0.0-RC1 -- we ought to be
// able to remove this by the time 3.0.0 final rolls around
ThisBuild / resolvers += Resolver.JCenterRepository

val mimaSettings = Seq(
  mimaPreviousArtifacts := previousVersion.map(organization.value %% name.value % _).toSet
)

ThisBuild / sonatypeProfileName := "com.typesafe"

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

def commonSettings =
  Seq(
    organization := "com.typesafe.play",
    organizationName := "Lightbend Inc.",
    organizationHomepage := Some(url("https://www.lightbend.com/")),
    homepage := Some(url(s"https://github.com/playframework/twirl")),
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    scalaVersion := Scala212,
    crossScalaVersions := ScalaVersions,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding",
      "utf8",
    ),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        Seq(
          "-Ywarn-unused:imports",
          "-Xlint:nullary-unit",
          "-Xlint",
          "-Ywarn-dead-code",
          // may help facilitate cross-building
          "-Xsource:3",
          // when we drop 2.12, this can be changed to `-Werror` which is the more modern name
          "-Xfatal-warnings",
        )
      case _ =>
        Seq()
    }),
    javacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-Xlint:-options",
      "-source",
      "1.8",
      "-target",
      "1.8",
      "-Xlint:deprecation",
      "-Xlint:unchecked",
      "-Werror",
    ),
    developers += Developer(
      "contributors",
      "Contributors",
      "https://gitter.im/playframework/contributors",
      url("https://github.com/playframework")
    ),
    description := "Twirl"
  )

lazy val twirl = project
  .in(file("."))
  .disablePlugins(MimaPlugin)
  .settings(
    crossScalaVersions := Nil, // workaround so + uses project-defined variants
    publish / skip := true
  )
  .aggregate(apiJvm, apiJs, parser, compiler) // but not plugin, .travis.yml deals with that separately

lazy val nodeJs = {
  if (System.getProperty("NODE_PATH") != null)
    new NodeJSEnv(NodeJSEnv.Config().withExecutable(System.getProperty("NODE_PATH")))
  else
    new NodeJSEnv()
}

lazy val api = crossProject(JVMPlatform, JSPlatform)
  .in(file("api"))
  .enablePlugins(Playdoc, Omnidoc)
  .configs(Docs)
  .settings(
    commonSettings,
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
  .enablePlugins(Omnidoc)
  .settings(
    commonSettings,
    mimaSettings,
    name := "twirl-parser",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % ScalaParserCombinatorsVersion % Optional,
    libraryDependencies += "com.novocode"            % "junit-interface"          % "0.11"                        % Test,
    libraryDependencies += "org.scalatest"         %%% "scalatest"                % ScalaTestVersion              % Test,
  )

lazy val compiler = project
  .in(file("compiler"))
  .enablePlugins(Omnidoc)
  .dependsOn(apiJvm, parser % "compile;test->test")
  .settings(
    commonSettings,
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
  .enablePlugins(SbtPlugin)
  .dependsOn(compiler)
  .settings(
    name := "sbt-twirl",
    organization := "com.typesafe.sbt",
    scalaVersion := Scala212,
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalaTestVersion % Test,
    Compile / resourceGenerators += generateVersionFile.taskValue,
    scriptedLaunchOpts += version.apply { v => s"-Dproject.version=$v" }.value,
    scriptedDependencies := {
      scriptedDependencies.value
      publishLocal.all(ScopeFilter(inAnyProject)).value
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
