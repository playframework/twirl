name := "Play2 Template Parser"

crossScalaVersions := Seq("2.9.3", "2.10.2")

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

// specs2 resolvers
resolvers ++= Seq("snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                  "releases"  at "http://oss.sonatype.org/content/repositories/releases")

// specs2 dependency
libraryDependencies <+= scalaVersion {
  case "2.9.3" => "org.specs2" %% "specs2" % "1.12.4.1" % "test"
  case "2.10.1" | "2.10.2" => "org.specs2" %% "specs2" % "2.1.1" % "test"
}

// scala-library dependency
libraryDependencies <+= scalaVersion {
  case version => "org.scala-lang" % "scala-library" % version
}
