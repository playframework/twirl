addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")

// For the Cross Build
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.6.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0")

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.9.2")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.6.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"    % "2.4.3")
