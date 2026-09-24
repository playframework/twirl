// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val plugins = project.in(file(".")).dependsOn(sbtTwirl)

lazy val sbtTwirl = ProjectRef(Path.fileProperty("user.dir").getParentFile, "plugin")

resolvers += Resolver.sonatypeCentralSnapshots

addSbtPlugin("org.playframework" % "play-docs-sbt-plugin" % sys.props.getOrElse("play.version", "3.1.0-M10-e1f3c2a9-SNAPSHOT"))
addSbtPlugin("com.github.sbt"    % "sbt-header"           % "5.11.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"         % "2.6.2")
