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
  ).settings(overrideTwirlSettings: _*)
  .dependsOn(twirlApi)

// The changes in Twirl imports cause a problem with the PlayDocsPlugin, which defines its own twirl compile tasks
// and doesn't use the default imports provided by Twirl but defines its own by scratch, and since the defaults
// have changed, this breaks.  So, first we need to set all source generators in test to Nil, then we can redefine the
// twirl settings.
def overrideTwirlSettings: Seq[Setting[_]] = Seq(
  sourceGenerators in Test := Nil
) ++ inConfig(Test)(SbtTwirl.twirlSettings) ++ SbtTwirl.defaultSettings ++ SbtTwirl.positionSettings ++ Seq(
  sourceDirectories in (Test, TwirlKeys.compileTemplates) ++=
    (PlayDocsKeys.javaManualSourceDirectories.value ++ PlayDocsKeys.scalaManualSourceDirectories.value)
)

// the twirl plugin automatically adds this dependency, but this overrides it so
// it can be an interproject dependency, rather than requiring it to be published
// first
lazy val twirlApi = ProjectRef(Path.fileProperty("user.dir").getParentFile, "api")
