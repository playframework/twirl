import sbtcrossproject.crossProject
import org.scalajs.jsenv.nodejs.NodeJSEnv

val scala210 = "2.10.7"
val scala211 = "2.11.12"
val scala212 = "2.12.15"
val scala213 = "2.13.6"

def binaryCompatibilitySettings(org: String, moduleName: String, scalaBinVersion: String): Set[ModuleID] = {
  val artifact = org % s"${moduleName}_${scalaBinVersion}"
  if (scala213.startsWith(scalaBinVersion)) Set(artifact % "1.4.2")
  else Set(artifact                                      % "1.4.0")
}

val headerSettings = Seq(
  headerLicense := {
    val currentYear = java.time.Year.now(java.time.Clock.systemUTC).getValue
    Some(
      HeaderLicense.Custom(
        s"Copyright (C) 2009-$currentYear Lightbend Inc. <https://www.lightbend.com>"
      )
    )
  },
  headerEmptyLine := false
)

val commonSettings = headerSettings ++ Seq(
  scalaVersion := scala212,
  crossScalaVersions := Seq(scala210, "2.11.12", scala212, scala213)
)

lazy val twirl = project
  .in(file("."))
  .enablePlugins(PlayRootProject)
  .settings(commonSettings)
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
  .settings(commonSettings: _*)
  .settings(
    name := "twirl-api",
    jsEnv := nodeJs,
    libraryDependencies ++= scalaXml.value,
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest(scalaVersion.value) % "test",
    mimaPreviousArtifacts := binaryCompatibilitySettings(
      organization.value,
      moduleName.value,
      scalaBinaryVersion.value
    ),
  )

lazy val apiJvm = api.jvm
lazy val apiJs  = api.js

lazy val parser = project
  .in(file("parser"))
  .enablePlugins(PlayLibrary)
  .settings(commonSettings: _*)
  .settings(
    name := "twirl-parser",
    libraryDependencies ++= scalaParserCombinators(scalaVersion.value),
    libraryDependencies += "com.novocode"  % "junit-interface" % "0.11"                        % "test",
    libraryDependencies += "org.scalatest" %%% "scalatest"     % scalatest(scalaVersion.value) % "test",
    mimaPreviousArtifacts := binaryCompatibilitySettings(organization.value, moduleName.value, scalaBinaryVersion.value)
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
    fork in run := true,
    mimaPreviousArtifacts := binaryCompatibilitySettings(
      organization.value,
      moduleName.value,
      scalaBinaryVersion.value
    ),
  )

lazy val plugin = project
  .in(file("sbt-twirl"))
  .enablePlugins(PlaySbtPlugin, SbtPlugin)
  .dependsOn(compiler)
  .settings(headerSettings)
  .settings(
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
