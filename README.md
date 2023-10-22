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
[![Maven](https://img.shields.io/maven-central/v/org.playframework.twirl/twirl-api_2.13.svg?logo=apache-maven)](https://mvnrepository.com/artifact/org.playframework.twirl/twirl-api_2.13)
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
// twirl 2.0 and newer:
addSbtPlugin("org.playframework.twirl" % "sbt-twirl" % "LATEST_VERSION")
// twirl 1.6:
addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.6.1")
// twirl 1.5.1 and before:
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

## maven-twirl

To use the Twirl plugin in your project add the Maven plugin and
Twirl API as a dependency into `pom.xml`:

```pom
<dependencies>
    <dependency>
        <groupId>org.playframework.twirl</groupId>
        <artifactId>twirl-api_${SCALA_VERSION}</artifactId>
        <version>${TWIRL_VERSION}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.playframework.twirl</groupId>
            <artifactId>twirl-maven-plugin_${SCALA_VERSION}</artifactId>
            <version>${TWIRL_VERSION}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Replacing the `TWIRL_VERSION` with the latest version published, which should be [![Latest version](https://index.scala-lang.org/playframework/twirl/twirl-api/latest.svg?color=orange)](https://index.scala-lang.org/playframework/twirl/twirl-api).

### Template files

Twirl template files are expected to be placed under `src/main/twirl` or
`src/test/twirl`, similar to `scala` or `java` sources. The additional source
locations for template files can be configured.

Template files must be named `{name}.scala.{ext}` where `ext` can be `html`,
`js`, `xml`, or `txt`.

### Additional imports

To add additional imports for the Scala code in template files, use the
`templateImports` parameter. For example:

```pom
<plugin>
    <groupId>org.playframework.twirl</groupId>
    <artifactId>twirl-maven-plugin_${SCALA_VERSION}</artifactId>
    <version>${TWIRL_VERSION}</version>
    <configuration>
        <templateImports>
            <import>org.example._</import>
        </templateImports>
    </configuration>
</plugin>
```

### Source directories

To configure the source directories where template files will be found, use the
`sourceDir` parameter. For example:

```pom
<plugin>
    <groupId>org.playframework.twirl</groupId>
    <artifactId>twirl-maven-plugin_${SCALA_VERSION}</artifactId>
    <version>${TWIRL_VERSION}</version>
    <configuration>
        <sourceDir>${project.basedir}/src/main/templates</sourceDir>
    </configuration>
    <executions>
        <execution>
            <id>additional-source-directory</id>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <sourceDir>${project.basedir}/src/main/other-templates</sourceDir>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Scala version

To configure the Scala version just use the suffix in `artifactId`.

### Other properties

Also, you can use the next parameters:

```pom
<plugin>
    <groupId>org.playframework.twirl</groupId>
    <artifactId>twirl-maven-plugin_${SCALA_VERSION}</artifactId>
    <version>${TWIRL_VERSION}</version>
    <configuration>
        <constructorAnnotations>
            <annotation>@org.example.MyAnnotation()</annotation>
        </constructorAnnotations>
        <templateFormats>
            <csv>play.twirl.api.TxtFormat</csv>
        </templateFormats>
        <sourceEncoding>UTF-8</sourceEncoding>
    </configuration>
</plugin>
```

### Snapshots

To use a snapshot version add the [Sonatype Snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/org/playframework/twirl/) into `pom.xml`:

```pom
<pluginRepositories>
    <pluginRepository>
        <id>sonatype-snapshots</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```

## gradle-twirl

⚠️ `org.playframework.twirl` plugin requires Gradle 7.1 or higher.

To use the Twirl plugin in your project add the gradle plugin and 
Twirl API as a dependency into `build.gradle.kts`:

```kotlin
plugins {
  ...
  id("org.playframework.twirl") version "LATEST_VERSION"
}

dependencies {
  implementation("org.playframework.twirl", "twirl-api_${scalaVersion}", "LATEST_VERSION")
}
```

Replacing the `LATEST_VERSION` with the latest version published, which should be [![Latest version](https://index.scala-lang.org/playframework/twirl/twirl-api/latest.svg?color=orange)](https://index.scala-lang.org/playframework/twirl/twirl-api).

### Template files

Twirl template files are expected to be placed under `src/main/twirl` or
`src/test/twirl`, similar to `scala` or `java` sources. The additional source 
locations for template files can be configured.

Template files must be named `{name}.scala.{ext}` where `ext` can be `html`,
`js`, `xml`, or `txt`.

### Additional imports

To add additional imports for the Scala code in template files, use the
`templateImports` key. For example:

```kotlin
sourceSets {
  main {
    twirl {
      templateImports.add("org.example._")
    }
  }
}
```

### Source directories

To configure the source directories where template files will be found, use the
`srcDir` method for [SourceDirectorySet](https://docs.gradle.org/current/javadoc/org/gradle/api/file/SourceDirectorySet.html). For example:

```kotlin
sourceSets {
  main {
    twirl {
      srcDir("app")
    }
  }
}
```

### Scala version

To configure the Scala version use the `scalaVersion` property of [TwirlExtension](gradle-twirl/src/main/java/play/twirl/gradle/TwirlExtension.java) (`2.13` by default).  For example:

```kotlin
twirl {
  scalaVersion.set("3")
}
```

### Other properties

Also, you can use the next properties:

```kotlin
sourceSets {
  main {
    twirl {
      // Annotations added to constructors in injectable templates
      constructorAnnotations.add("@org.example.MyAnnotation()")
      // Defined custom twirl template formats
      templateFormats.put("csv", "play.twirl.api.TxtFormat")
      // Source encoding for template files and generated scala files
      sourceEncoding.set("<enc>")
    }
  }
}
```

### Snapshots

To use a snapshot version add the [Sonatype Snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/org/playframework/twirl/org.playframework.twirl.gradle.plugin/) into `settings.gradle.kts`:

```kotlin
pluginManagement {
  repositories {
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }
}
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
