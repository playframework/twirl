// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

// For the Cross Build
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.15.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin"    % "1.1.4")
addSbtPlugin("de.heikoseeberger" % "sbt-header"         % "5.10.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.5.5")
addSbtPlugin("com.github.sbt"    % "sbt-java-formatter" % "0.10.0")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"          % "2.0.13")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"      % "0.13.1")
