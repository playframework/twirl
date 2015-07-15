lazy val docs = project
  .in(file("."))
  .enablePlugins(PlayDocsPlugin)
  .settings(
    // use special snapshot play version for now
    resolvers ++= DefaultOptions.resolvers(snapshot = true),
    resolvers += Resolver.typesafeRepo("releases"),
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases", // specs2 depends on scalaz-stream
    libraryDependencies += component("play-test") % "test",
    libraryDependencies += component("play-specs2") % "test",
    PlayDocsKeys.javaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "javaGuide" ** "code").get,
    PlayDocsKeys.scalaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "scalaGuide" ** "code").get
  ).dependsOn(twirlApi)

// the twirl plugin automatically adds this dependency, but this overrides it so
// it can be an interproject dependency, rather than requiring it to be published
// first
lazy val twirlApi = ProjectRef(Path.fileProperty("user.dir").getParentFile, "api")
