lazy val twirl = project
  .in(file("."))
  .aggregate(api, parser)
  .settings(commonSettings: _*)

lazy val api = project
  .in(file("api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"
  )

lazy val parser = project
  .in(file("parser"))
  .dependsOn(api)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies <+= scalaBinaryVersion {
      case "2.9.3" => "org.specs2" %% "specs2" % "1.12.4.1" % "test"
      case "2.10"  => "org.specs2" %% "specs2" % "2.1.1" % "test"
    }
  )

lazy val commonSettings = Seq(
  scalaVersion := "2.10.4",
  crossScalaVersions := Seq("2.9.3", "2.10.4"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
  unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / ("scala-" + scalaBinaryVersion.value)
)
