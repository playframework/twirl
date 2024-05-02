// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.LineCommentCreator

lazy val docs = project
  .in(file("."))
  .enablePlugins(PlayDocsPlugin)
  .configs(Configuration.of("Docs", "docs"))
  .settings(
    scalaVersion := "2.13.14",
    // use special snapshot play version for now
    resolvers ++= DefaultOptions.resolvers(snapshot = true),
    libraryDependencies += component("play-test")   % "test",
    libraryDependencies += component("play-specs2") % "test",
    PlayDocsKeys.javaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "javaGuide" ** "code").get,
    PlayDocsKeys.scalaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "scalaGuide" ** "code").get,
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
    (Compile / headerSources) ++=
      ((baseDirectory.value ** ("*.properties" || "*.sbt" || "*.md" || "*.scala")) --- (baseDirectory.value ** "target" ** "*")).get
  )
  .settings(overrideTwirlSettings: _*)
  .dependsOn(twirlApi)

// The changes in Twirl imports cause a problem with the PlayDocsPlugin, which defines its own twirl compile tasks
// and doesn't use the default imports provided by Twirl but defines its own by scratch, and since the defaults
// have changed, this breaks.  So, first we need to set all source generators in test to Nil, then we can redefine the
// twirl settings.
def overrideTwirlSettings: Seq[Setting[_]] =
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
