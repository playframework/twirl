// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween
import sbtheader.CommentStyle
import sbtheader.FileType
import sbtheader.LineCommentCreator

// The docs are a separate sbt build and cannot reuse the root build's Dependencies object.
val scala33LTSVersion  = "3.3.8"
val playVersion        = "3.1.0-M4"
val currentPlayVersion = "3.1.0-M9"

lazy val docs = project
  .in(file("."))
  .enablePlugins(PlayDocsPlugin)
  .configs(Configuration.of("Docs", "docs"))
  .settings(
    scalaVersion := sys.props.getOrElse("scala.version", scala33LTSVersion),
    scalacOptions ++= {
      if (scalaVersion.value.startsWith("3.3.")) Seq("-release:17", "-Yfuture-lazy-vals") else Seq.empty
    },
    // use special snapshot play version for now
    resolvers ++= DefaultOptions.resolvers(snapshot = true),
    // M4 is the newest Play 3.1 milestone whose Scala 3 artifacts were built with Scala 3.3.
    // TODO: After Play 3.1.0-M10 is released with Scala 3.3, upgrade the docs plugin and
    // restore component("play-test") and component("play-specs2"), then remove this rewrite.
    libraryDependencies += "org.playframework" %% "play-test"   % playVersion % Test,
    libraryDependencies += "org.playframework" %% "play-specs2" % playVersion % Test,
    // PlayDocsPlugin M9 injects Play M9 libraries even when the explicit test dependencies use M4.
    // Keep the sbt 2-compatible plugin, but make its application dependencies readable by Scala 3.3.
    libraryDependencies ~= (_.map {
      case dependency
          if dependency.organization == "org.playframework" && dependency.revision == currentPlayVersion =>
        dependency.withRevision(playVersion)
      case dependency => dependency
    }),
    PlayDocsKeys.javaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "javaGuide" ** "code").get(),
    PlayDocsKeys.scalaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "scalaGuide" ** "code").get(),
    headerLicense := {
      Some(
        HeaderLicense.Custom(
          s"Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
        )
      )
    },
    headerMappings ++= Map(
      FileType("sbt")        -> HeaderCommentStyle.cppStyleLineComment,
      FileType("properties") -> HeaderCommentStyle.hashLineComment,
      FileType("md") -> CommentStyle(new LineCommentCreator("<!---", "-->"), commentBetween("<!---", "*", "-->")),
    ),
    (Compile / headerSources) ++= Def.uncached(
      ((baseDirectory.value ** ("*.properties" || "*.sbt" || "*.md" || "*.scala")) --- (baseDirectory.value ** "target" ** "*")).get()
    )
  )
  .settings(overrideTwirlSettings)
  .dependsOn(twirlApi)

// The changes in Twirl imports cause a problem with the PlayDocsPlugin, which defines its own twirl compile tasks
// and doesn't use the default imports provided by Twirl but defines its own by scratch, and since the defaults
// have changed, this breaks.  So, first we need to set all source generators in test to Nil, then we can redefine the
// twirl settings.
def overrideTwirlSettings: Seq[Setting[?]] =
  Seq(
    Test / sourceGenerators := Nil
  ) ++ inConfig(Test)(SbtTwirl.twirlSettings) ++ SbtTwirl.defaultSettings ++ SbtTwirl.positionSettings ++ Seq(
    Test / TwirlKeys.compileTemplates / sourceDirectories ++=
      (PlayDocsKeys.javaManualSourceDirectories.value ++ PlayDocsKeys.scalaManualSourceDirectories.value)
  )

// the twirl plugin automatically adds this dependency, but this overrides it so
// it can be an interproject dependency, rather than requiring it to be published
// first
lazy val twirlApi = ProjectRef(Path.fileProperty("user.dir").getParentFile, "apiJVM")
