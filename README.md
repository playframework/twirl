<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Twirl

[![Twitter Follow](https://img.shields.io/twitter/follow/playframework?label=follow&style=flat&logo=twitter&color=brightgreen)](https://twitter.com/playframework)
[![Discord](https://img.shields.io/discord/931647755942776882?logo=discord&logoColor=white)](https://discord.gg/g5s2vtZ4Fa)
[![GitHub Discussions](https://img.shields.io/github/discussions/playframework/playframework?&logo=github&color=brightgreen)](https://github.com/playframework/playframework/discussions)
[![StackOverflow](https://img.shields.io/static/v1?label=stackoverflow&logo=stackoverflow&logoColor=fe7a16&color=brightgreen&message=playframework)](https://stackoverflow.com/tags/playframework)
[![YouTube](https://img.shields.io/youtube/channel/views/UCRp6QDm5SDjbIuisUpxV9cg?label=watch&logo=youtube&style=flat&color=brightgreen&logoColor=ff0000)](https://www.youtube.com/channel/UCRp6QDm5SDjbIuisUpxV9cg)
[![Twitch Status](https://img.shields.io/twitch/status/playframework?logo=twitch&logoColor=white&color=brightgreen&label=live%20stream)](https://www.twitch.tv/playframework)
[![OpenCollective](https://img.shields.io/opencollective/all/playframework?label=financial%20contributors&logo=open-collective)](https://opencollective.com/playframework)

[![Build Status](https://github.com/playframework/twirl/actions/workflows/build-test.yml/badge.svg)](https://github.com/playframework/twirl/actions/workflows/build-test.yml)
[![Maven](https://img.shields.io/maven-central/v/com.typesafe.play/twirl-api_2.13.svg?logo=apache-maven)](https://mvnrepository.com/artifact/com.typesafe.play/twirl-api_2.13)
[![Repository size](https://img.shields.io/github/repo-size/playframework/twirl.svg?logo=git)](https://github.com/playframework/twirl)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://api.mergify.com/v1/badges/playframework/twirl&style=flat)](https://mergify.com)

Twirl is the [Play][play-site] [template engine][docs].

Twirl is automatically available in Play projects and can also be used
stand-alone without any dependency on Play.

See the Play [documentation for the template engine][docs] for more information
about the template syntax.

## sbt-twirl

Twirl can also be used outside of Play. An sbt plugin is provided for easy
integration with Scala or Java projects.

> sbt-twirl requires sbt 1.3.0 or higher.

To add the sbt plugin to your project add the sbt plugin dependency in
`project/plugins.sbt`:

```scala
// twirl 1.6 and newer:
addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "LATEST_VERSION")
// twirl 1.5 and before:
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.5.1")
```

Replacing the `LATEST_VERSION` with the latest version published, which should be [![Latest version](https://index.scala-lang.org/playframework/twirl/twirl-api/latest.svg?color=orange)](https://index.scala-lang.org/playframework/twirl/twirl-api). And enable the plugin on projects using:

```scala
someProject.enablePlugins(SbtTwirl)
```

If you only have a single project and are using a `build.sbt` file, create a
root project and enable the twirl plugin like this:

```scala
lazy val root = (project in file(".")).enablePlugins(SbtTwirl)
```

### Template files

Twirl template files are expected to be placed under `src/main/twirl` or
`src/test/twirl`, similar to `scala` or `java` sources. The source locations for
template files can be configured.

Template files must be named `{name}.scala.{ext}` where `ext` can be `html`,
`js`, `xml`, or `txt`.

The Twirl template compiler is automatically added as a source generator for
both the `main`/`compile` and `test` configurations. When you run `compile` or
`Test/compile` the Twirl compiler will generate Scala source files from the
templates and then these Scala sources will be compiled along with the rest of
your project.

### Additional imports

To add additional imports for the Scala code in template files, use the
`templateImports` key. For example:

```scala
TwirlKeys.templateImports += "org.example._"
```

### Source directories

To configure the source directories where template files will be found, use the
`compileTemplates / sourceDirectories` key. For example, to have template
sources alongside Scala or Java source files:

```scala
Compile / TwirlKeys.compileTemplates / sourceDirectories := (Compile / unmanagedSourceDirectories).value
```

## Releasing a new version

See https://github.com/playframework/.github/blob/main/RELEASING.md

## Credits

The name *twirl* was thought up by the [Spray team][spray] and refers to the
magic `@` character in the template language, which is sometimes called "twirl".

The first stand-alone version of Twirl was created by the [Spray team][spray].

An optimized version of the Twirl parser was contributed by the
[Scala IDE team][scala-ide].

[play-site]: https://www.playframework.com
[docs]: https://www.playframework.com/documentation/latest/ScalaTemplates
[spray]: https://github.com/spray
[scala-ide]: https://github.com/scala-ide
