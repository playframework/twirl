lazy val twirl = project
  .in(file("."))
  .aggregate(api, parser, compiler)
  .settings(commonSettings: _*)

lazy val api = project
  .in(file("api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies += commonsLang
  )

lazy val parser = project
  .in(file("parser"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies += specs2(scalaBinaryVersion.value)
  )

lazy val compiler = project
  .in(file("compiler"))
  .dependsOn(api, parser % "compile;test->test")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies += scalaCompiler(scalaVersion.value),
    libraryDependencies += scalaIO(scalaBinaryVersion.value)
  )

// Shared settings

lazy val commonSettings = Seq(
  scalaVersion := "2.10.4",
  crossScalaVersions := Seq("2.9.3", "2.10.4"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
  unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / ("scala-" + scalaBinaryVersion.value)
)

// Dependencies

def commonsLang = "org.apache.commons" % "commons-lang3" % "3.1"

def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version

def scalaIO(scalaVersion: String) = scalaVersion match {
  case "2.9.3" => "com.github.scala-incubator.io" % "scala-io-file_2.9.2"  % "0.4.1-seq"
  case "2.10" => "com.github.scala-incubator.io" %% "scala-io-file"  % "0.4.2"
}

def specs2(scalaBinaryVersion: String) = scalaBinaryVersion match {
  case "2.9.3" => "org.specs2" %% "specs2" % "1.12.4.1" % "test"
  case "2.10"  => "org.specs2" %% "specs2" % "2.3.10" % "test"
}
