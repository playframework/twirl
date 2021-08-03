addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % sys.props("project.version"))
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.0")

// Next line can be removed when dropping Scala 2.12? See https://github.com/playframework/twirl/pull/424
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

