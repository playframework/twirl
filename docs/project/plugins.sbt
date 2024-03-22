// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val plugins = (project in file(".")).dependsOn(sbtTwirl)

lazy val sbtTwirl = ProjectRef(Path.fileProperty("user.dir").getParentFile, "plugin")

resolvers ++= DefaultOptions.resolvers(snapshot = true)

addSbtPlugin("org.playframework" % "play-docs-sbt-plugin" % sys.props.getOrElse("play.version", "3.0.2"))
addSbtPlugin("de.heikoseeberger" % "sbt-header"           % "5.10.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"         % "2.5.2")
