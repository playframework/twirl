lazy val specs2 = Seq(
  "org.specs2" %% "specs2-core"  % "3.6" % "test",
  "org.specs2" %% "specs2-junit" % "3.6" % "test",
  "org.specs2" %% "specs2-mock"  % "3.6" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "3.6" % "test"
)

lazy val twirl = project
  .in(file("."))
  .enablePlugins(PlayRootProject)
  .aggregate(api, parser, compiler)
  .settings(common: _*)

lazy val api = project
  .in(file("api"))
  .enablePlugins(PlayLibrary, Playdoc)
  .settings(common: _*)
  .settings(
    name := "twirl-api",
    libraryDependencies += "org.unbescape" % "unbescape" % "1.1.1.RELEASE",
    libraryDependencies ++= scalaXml(scalaVersion.value),
    libraryDependencies ++= specs2
  )

lazy val parser = project
  .in(file("parser"))
  .enablePlugins(PlayLibrary)
  .settings(common: _*)
  .settings(
    name := "twirl-parser",
    libraryDependencies ++= scalaParserCombinators(scalaVersion.value),
    libraryDependencies ++= specs2
  )

lazy val compiler = project
  .in(file("compiler"))
  .enablePlugins(PlayLibrary)
  .dependsOn(api, parser % "compile;test->test")
  .settings(common: _*)
  .settings(
    name := "twirl-compiler",
    libraryDependencies += scalaCompiler(scalaVersion.value),
    fork in run := true
  )

lazy val plugin = project
  .in(file("sbt-twirl"))
  .enablePlugins(PlaySbtPlugin)
  .dependsOn(compiler)
  .settings(common: _*)
  .settings(
    name := "sbt-twirl",
    organization := "com.typesafe.sbt",
    libraryDependencies ++= specs2,
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

// Shared settings

def common = Seq(
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
)

// Version file

def generateVersionFile = Def.task {
  val version = (Keys.version in api).value
  val file = (resourceManaged in Compile).value / "twirl.version.properties"
  val content = s"twirl.api.version=$version"
  IO.write(file, content)
  Seq(file)
}

// Dependencies

def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version

def scalaParserCombinators(scalaVersion: String) =
  whenAtLeast(scalaVersion, 2, 11, "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1" % "optional")

def scalaXml(scalaVersion: String) =
  whenAtLeast(scalaVersion, 2, 11, "org.scala-lang.modules" %% "scala-xml" % "1.0.1")

def whenAtLeast(version: String, major: Int, minor: Int, module: ModuleID): Seq[ModuleID] = {
  CrossVersion.partialVersion(version) match {
    case Some((x, y)) if x > major || (x == major && y >= minor) => Seq(module)
    case _ => Seq.empty
  }
}
