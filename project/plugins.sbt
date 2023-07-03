// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

// For the Cross Build
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.13.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.1")

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin"    % "1.1.2")
addSbtPlugin("de.heikoseeberger" % "sbt-header"         % "5.10.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.5.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.8.0")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"          % "1.5.7")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"      % "0.11.0")
