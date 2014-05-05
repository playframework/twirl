lazy val twirl = project
  .in(file("."))
  .aggregate(api, parser, compiler)
  .settings(common: _*)
  .settings(crossScala: _*)
  .settings(noPublish: _*)

lazy val api = project
  .in(file("api"))
  .settings(common: _*)
  .settings(crossScala: _*)
  .settings(publishMaven: _*)
  .settings(
    name := "twirl-api",
    libraryDependencies += commonsLang,
    libraryDependencies += specs2(scalaBinaryVersion.value),
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
        case _ =>
          libraryDependencies.value
      }
    }
  )

lazy val parser = project
  .in(file("parser"))
  .settings(common: _*)
  .settings(crossScala: _*)
  .settings(publishMaven: _*)
  .settings(
    name := "twirl-parser",
    libraryDependencies += specs2(scalaBinaryVersion.value),
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1" % "optional"
        case _ =>
          libraryDependencies.value
      }
    }
  )

lazy val compiler = project
  .in(file("compiler"))
  .dependsOn(api, parser % "compile;test->test")
  .settings(common: _*)
  .settings(crossScala: _*)
  .settings(publishMaven: _*)
  .settings(
    name := "twirl-compiler",
    libraryDependencies += scalaCompiler(scalaVersion.value),
    fork in run := true
  )

lazy val plugin = project
  .in(file("sbt-twirl"))
  .dependsOn(compiler)
  .settings(common: _*)
  .settings(scriptedSettings: _*)
  .settings(publishSbtPlugin: _*)
  .settings(
    name := "sbt-twirl",
    organization := "com.typesafe.sbt",
    sbtPlugin := true,
    scriptedLaunchOpts += ("-Dproject.version=" + version.value),
    scriptedLaunchOpts += "-XX:MaxPermSize=256m",
    resourceGenerators in Compile <+= generateVersionFile
  )

// Shared settings

def common = Seq(
  organization := "com.typesafe.play",
  version := "1.0-M3",
  scalaVersion := sys.props.get("scala.version").getOrElse("2.10.4"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
)

def crossScala = Seq(
  crossScalaVersions := Seq("2.9.3", "2.10.4", "2.11.0"),
  unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / ("scala-" + scalaBinaryVersion.value)
)

def publishMaven = Seq(
  publishTo := {
    if (isSnapshot.value) Some(Opts.resolver.sonatypeSnapshots)
    else Some(Opts.resolver.sonatypeStaging)
  },
  homepage := Some(url("https://github.com/playframework/twirl")),
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  pomExtra := {
    <scm>
      <url>https://github.com/playframework/twirl</url>
      <connection>scm:git:git@github.com:playframework/twirl.git</connection>
    </scm>
    <developers>
      <developer>
        <id>playframework</id>
        <name>Play Framework Team</name>
        <url>https://github.com/playframework</url>
      </developer>
    </developers>
  },
  pomIncludeRepository := { _ => false }
)

def publishSbtPlugin = Seq(
  publishMavenStyle := false,
  publishTo := {
    if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
    else Some(Classpaths.sbtPluginReleases)
  }
)

def noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishTo := Some(Resolver.file("no-publish", crossTarget.value / "no-publish"))
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

def commonsLang = "org.apache.commons" % "commons-lang3" % "3.1"

def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version

def specs2(scalaBinaryVersion: String) = scalaBinaryVersion match {
  case "2.9.3" => "org.specs2" %% "specs2" % "1.12.4.1" % "test"
  case "2.10" | "2.11" => "org.specs2" %% "specs2" % "2.3.11" % "test"
}
