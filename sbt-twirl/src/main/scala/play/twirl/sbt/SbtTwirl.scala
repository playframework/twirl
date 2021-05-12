/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.sbt

import play.twirl.compiler.TwirlCompiler
import sbt.Keys._
import sbt._

import scala.io.Codec

object Import {
  object TwirlKeys {
    val twirlVersion    = SettingKey[String]("twirl-version", "Twirl version used for twirl-api dependency")
    val templateFormats = SettingKey[Map[String, String]]("twirl-template-formats", "Defined twirl template formats")
    val templateImports = SettingKey[Seq[String]]("twirl-template-imports", "Extra imports for twirl templates")
    val constructorAnnotations = SettingKey[Seq[String]](
      "twirl-constructor-annotations",
      "Annotations added to constructors in injectable templates"
    )
    @deprecated("No longer supported", "1.2.0")
    val useOldParser = SettingKey[Boolean]("twirl-use-old-parser", "No longer supported")
    val sourceEncoding =
      TaskKey[String]("twirl-source-encoding", "Source encoding for template files and generated scala files")
    val compileTemplates =
      TaskKey[Seq[File]]("twirl-compile-templates", "Compile twirl templates into scala source files")
  }
}

object SbtTwirl extends AutoPlugin {
  import Import.TwirlKeys._

  val autoImport = Import

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = noTrigger

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(twirlSettings) ++
      inConfig(Test)(twirlSettings) ++
      defaultSettings ++
      positionSettings ++
      dependencySettings

  def twirlSettings: Seq[Setting[_]] =
    Seq(
      compileTemplates / includeFilter := "*.scala.*",
      compileTemplates / excludeFilter := HiddenFileFilter,
      compileTemplates / sourceDirectories := Seq(sourceDirectory.value / "twirl"),
      watchSources in Defaults.ConfigGlobal +=
        WatchSource(
          (compileTemplates / sourceDirectory).value,
          (compileTemplates / includeFilter).value,
          (compileTemplates / excludeFilter).value
        ),
      compileTemplates / sources := Defaults
        .collectFiles(
          compileTemplates / sourceDirectories,
          compileTemplates / includeFilter,
          compileTemplates / excludeFilter,
        )
        .value,
      compileTemplates / target := crossTarget.value / "twirl" / Defaults.nameForSrc(configuration.value.name),
      compileTemplates := compileTemplatesTask.value,
      sourceGenerators += compileTemplates.taskValue,
      managedSourceDirectories += (compileTemplates / target).value
    )

  def defaultSettings: Seq[Setting[_]] =
    Seq(
      templateFormats := defaultFormats,
      templateImports := TwirlCompiler.DefaultImports,
      constructorAnnotations := Nil,
      sourceEncoding := scalacEncoding(scalacOptions.value)
    )

  def positionSettings: Seq[Setting[_]] =
    Seq(
      sourcePositionMappers += TemplateProblem.positionMapper(Codec(sourceEncoding.value))
    )

  def dependencySettings =
    Def.settings(
      twirlVersion := readResourceProperty("twirl.version.properties", "twirl.api.version"),
      libraryDependencies += {
        val crossVer = crossVersion.value
        val isScalaJS = CrossVersion(crossVer, scalaVersion.value, scalaBinaryVersion.value) match {
          case Some(f) => f("").contains("_sjs1") // detect ScalaJS CrossVersion
          case None    => false
        }
        // TODO: can we use %%% from sbt-crossproject now that we're on Scala.js 1.x?
        val baseModuleID = "com.typesafe.play" %% "twirl-api" % twirlVersion.value
        if (isScalaJS) baseModuleID.cross(crossVer) else baseModuleID
      }
    )

  def scalacEncoding(options: Seq[String]): String = {
    val i = options.indexOf("-encoding") + 1
    if (i > 0 && i < options.length) options(i) else "UTF-8"
  }

  def defaultFormats =
    Map(
      "html" -> "play.twirl.api.HtmlFormat",
      "txt"  -> "play.twirl.api.TxtFormat",
      "xml"  -> "play.twirl.api.XmlFormat",
      "js"   -> "play.twirl.api.JavaScriptFormat"
    )

  def compileTemplatesTask =
    Def.task {
      TemplateCompiler.compile(
        (compileTemplates / sourceDirectories).value,
        (compileTemplates / target).value,
        templateFormats.value,
        templateImports.value,
        constructorAnnotations.value,
        (compileTemplates / includeFilter).value,
        (compileTemplates / excludeFilter).value,
        Codec(sourceEncoding.value),
        streams.value.log
      )
    }

  def readResourceProperty(resource: String, property: String): String = {
    val props  = new java.util.Properties
    val stream = getClass.getClassLoader.getResourceAsStream(resource)
    try {
      props.load(stream)
    } catch {
      case e: Exception =>
    } finally {
      if (stream ne null) stream.close
    }
    props.getProperty(property)
  }
}
