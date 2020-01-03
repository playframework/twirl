import interplay.ScalaVersions._
import sbtcrossproject.crossProject
import org.scalajs.jsenv.nodejs.NodeJSEnv

// Binary compatibility is this version
val previousVersion: Option[String] = Some("1.5.0")

val mimaSettings = Seq(
  mimaPreviousArtifacts := previousVersion.map(organization.value %% name.value % _).toSet
)

val javacParameters = Seq(
  "-source",
  "1.8",
  "-target",
  "1.8",
  "-Xlint:deprecation",
  "-Xlint:unchecked"
)

val scalacBasicParams = Seq(
  "-target:jvm-1.8",
)

val scalacExtraParams = scalacBasicParams ++ Seq(
  "-Ywarn-unused:imports",
  "-Xlint:nullary-unit",
  "-Xlint",
  "-Ywarn-dead-code",
)

val javaCompilerSettings = Seq(
  javacOptions in Compile ++= javacParameters,
  javacOptions in Test ++= javacParameters,
)

def scalacCompilerSettings(scalaVer: String) =
  if (scalaVer.equals(scala210)) {
    scalacBasicParams
  } else {
    scalacExtraParams
  }

val headerSettings = Seq(
  headerLicense := {
    Some(
      HeaderLicense.Custom(
        s"Copyright (C) Lightbend Inc. <https://www.lightbend.com>"
      )
    )
  },
  headerEmptyLine := false
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
dynverVTagPrefix in ThisBuild := false

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

// this overrides interplay release settings with some adaptations for working with sbt-dynver
// moreover, it contain only bits necessary for twirl, nothing else
lazy val releaseSettings: Seq[Setting[_]] = Seq(
  // Release settings
  releaseCrossBuild := false,
  releaseProcess := {
    import ReleaseTransformations._

    def ifDefinedAndTrue(key: SettingKey[Boolean], step: State => State): State => State = { state =>
      Project.extract(state).getOpt(key in ThisBuild) match {
        case Some(true) => step(state)
        case _          => state
      }
    }

    Seq[ReleaseStep](
      checkSnapshotDependencies,
      runClean,
      releaseStepCommandAndRemaining("+test"),
      releaseStepTask(playBuildExtraTests in thisProjectRef.value),
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepTask(playBuildExtraPublish in thisProjectRef.value),
      ifDefinedAndTrue(playBuildPromoteBintray, releaseStepTask(bintrayRelease in thisProjectRef.value)),
      ifDefinedAndTrue(playBuildPromoteSonatype, releaseStepCommand("sonatypeBundleRelease")),
      pushChanges
    )
  }
)

val commonSettings = javaCompilerSettings ++ headerSettings ++ Seq(
  scalaVersion := scala212,
  crossScalaVersions := Seq(scala210, scala212, scala213),
  scalacOptions ++= scalacCompilerSettings(scalaVersion.value),
)

lazy val twirl = project
  .in(file("."))
  .enablePlugins(PlayRootProject)
  .settings(commonSettings)
  .settings(releaseSettings)
  .settings(
    crossScalaVersions := Nil, // workaround so + uses project-defined variants
    releaseCrossBuild := false,
    mimaFailOnNoPrevious := false
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
  .enablePlugins(PlayLibrary, Playdoc)
  .configs(Docs)
  .settings(
    commonSettings,
    mimaSettings,
    name := "twirl-api",
    jsEnv := nodeJs,
    libraryDependencies ++= scalaXml.value,
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest(scalaVersion.value) % "test",
  )

lazy val apiJvm = api.jvm
lazy val apiJs  = api.js

lazy val parser = project
  .in(file("parser"))
  .enablePlugins(PlayLibrary)
  .settings(
    commonSettings,
    mimaSettings,
    name := "twirl-parser",
    libraryDependencies ++= scalaParserCombinators(scalaVersion.value),
    libraryDependencies += "com.novocode"  % "junit-interface" % "0.11"                        % "test",
    libraryDependencies += "org.scalatest" %%% "scalatest"     % scalatest(scalaVersion.value) % "test",
  )

lazy val compiler = project
  .in(file("compiler"))
  .enablePlugins(PlayLibrary)
  .dependsOn(apiJvm, parser % "compile;test->test")
  .settings(
    commonSettings,
    mimaSettings,
    name := "twirl-compiler",
    libraryDependencies += scalaCompiler(scalaVersion.value),
    libraryDependencies ++= scalaParserCombinators(scalaVersion.value),
    fork in run := true,
  )

lazy val plugin = project
  .in(file("sbt-twirl"))
  .enablePlugins(PlaySbtPlugin, SbtPlugin)
  .dependsOn(compiler)
  .settings(
    javaCompilerSettings,
    headerSettings,
    name := "sbt-twirl",
    organization := "com.typesafe.sbt",
    scalaVersion := scala212,
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest(scalaVersion.value) % "test",
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
    scalacOptions ++= scalacCompilerSettings(scalaVersion.value),
    mimaFailOnNoPrevious := false,
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
  val file    = (resourceManaged in Compile).value / "twirl.version.properties"
  val content = s"twirl.api.version=$version"
  IO.write(file, content)
  Seq(file)
}

// Dependencies

def scalatest(scalaV: String): String = scalaV match {
  case _ => "3.0.8"
}

def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version

def scalaParserCombinators(scalaVersion: String): Seq[ModuleID] = scalaVersion match {
  case interplay.ScalaVersions.scala210 => Seq.empty
  case _ =>
    Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2" % "optional"
    )
}

def scalaXml = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((x, y)) if x > 2 || (x == 2 && y >= 11) =>
      Seq("org.scala-lang.modules" %%% "scala-xml" % "1.2.0")
    case _ =>
      Seq.empty
  }
}

addCommandAlias("validateCode", ";headerCheck;test:headerCheck;scalafmtCheckAll;scalafmtSbtCheck")
