lazy val scalatest = "3.0.0"

lazy val twirl = project
    .in(file("."))
    .enablePlugins(PlayRootProject)
    .aggregate(apiJvm, apiJs, parser, compiler)

lazy val api = crossProject
    .in(file("api"))
    .enablePlugins(PlayLibrary, Playdoc)
    .settings(
      name := "twirl-api",
      libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % "test"
    )
    .jvmSettings(
      // scala-xml can't work under ScalaJS
      libraryDependencies ++= scalaXml(scalaVersion.value)
    )

lazy val apiJvm = api.jvm
lazy val apiJs = api.js

lazy val parser = project
    .in(file("parser"))
    .enablePlugins(PlayLibrary)
    .settings(
      name := "twirl-parser",
      libraryDependencies ++= scalaParserCombinators(scalaVersion.value),
      libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % "test"
    )

lazy val compiler = project
    .in(file("compiler"))
    .enablePlugins(PlayLibrary)
    .dependsOn(apiJvm, parser % "compile;test->test")
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
      libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % "test",
      // Plugin for %%%
      addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.12"),
      resourceGenerators in Compile <+= generateVersionFile,
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

def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version

def scalaParserCombinators(scalaVersion: String) =
  whenAtLeast(scalaVersion, 2, 11, "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4" % "optional")

def scalaXml(scalaVersion: String) =
  whenAtLeast(scalaVersion, 2, 11, "org.scala-lang.modules" %% "scala-xml" % "1.0.5")

def whenAtLeast(version: String, major: Int, minor: Int, module: ModuleID): Seq[ModuleID] = {
  CrossVersion.partialVersion(version) match {
    case Some((x, y)) if x > major || (x == major && y >= minor) => Seq(module)
    case _ => Seq.empty
  }
}
