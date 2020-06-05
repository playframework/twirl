# Twirl [![Latest version](https://index.scala-lang.org/playframework/twirl/twirl-api/latest.svg?color=orange)](https://index.scala-lang.org/playframework/twirl/twirl-api) [![Build Status](https://travis-ci.org/playframework/twirl.svg)](https://travis-ci.org/playframework/twirl)

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
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "LATEST_VERSION")
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
`test:compile` the Twirl compiler will generate Scala source files from the
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
`sourceDirectories in compileTemplates` key. For example, to have template
sources alongside Scala or Java source files:

```scala
sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value
```

## Credits

The name *twirl* was thought up by the [Spray team][spray] and refers to the
magic `@` character in the template language, which is sometimes called "twirl".

The first stand-alone version of Twirl was created by the [Spray team][spray].

An optimized version of the Twirl parser was contributed by the
[Scala IDE team][scala-ide].

[play-site]: http://www.playframework.com
[docs]: http://www.playframework.com/documentation/latest/ScalaTemplates
[spray]: https://github.com/spray
[scala-ide]: https://github.com/scala-ide
