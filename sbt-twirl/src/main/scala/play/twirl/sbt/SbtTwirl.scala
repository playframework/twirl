/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.sbt

import play.twirl.compiler.TwirlCompiler
import sbt.Keys._
import sbt._

import scala.io.Codec

object Import {
  object TwirlKeys {
    val twirlVersion = SettingKey[String]("twirl-version", "Twirl version used for twirl-api dependency")
    val templateFormats = SettingKey[Map[String, String]]("twirl-template-formats", "Defined twirl template formats")
    val templateImports = SettingKey[Seq[String]]("twirl-template-imports", "Extra imports for twirl templates")
    val constructorAnnotations = SettingKey[Seq[String]]("twirl-constructor-annotations", "Annotations added to constructors in injectable templates")
    @deprecated("No longer supported", "1.2.0")
    val useOldParser = SettingKey[Boolean]("twirl-use-old-parser", "No longer supported")
    val sourceEncoding = TaskKey[String]("twirl-source-encoding", "Source encoding for template files and generated scala files")
    val compileTemplates = TaskKey[Seq[File]]("twirl-compile-templates", "Compile twirl templates into scala source files")
  }
}

object SbtTwirl extends AutoPlugin {

  import Import.TwirlKeys._

  val autoImport = Import

  import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = noTrigger

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(twirlSettings) ++
        inConfig(Test)(twirlSettings) ++
        defaultSettings ++
        positionSettings ++
        dependencySettings

  def twirlSettings: Seq[Setting[_]] = Seq(
    includeFilter in compileTemplates := "*.scala.*",
    excludeFilter in compileTemplates := HiddenFileFilter,
    sourceDirectories in compileTemplates := Seq(sourceDirectory.value / "twirl"),

    sources in compileTemplates <<= Defaults.collectFiles(
      sourceDirectories in compileTemplates,
      includeFilter in compileTemplates,
      excludeFilter in compileTemplates
    ),

    watchSources in Defaults.ConfigGlobal <++= sources in compileTemplates,

    target in compileTemplates := crossTarget.value / "twirl" / Defaults.nameForSrc(configuration.value.name),

    compileTemplates := compileTemplatesTask.value,

    sourceGenerators <+= compileTemplates,
    managedSourceDirectories <+= target in compileTemplates
  )

  def defaultSettings: Seq[Setting[_]] = Seq(
    templateFormats := defaultFormats,
    templateImports := TwirlCompiler.DefaultImports,
    constructorAnnotations := Nil,
    sourceEncoding := scalacEncoding(scalacOptions.value)
  )

  def positionSettings: Seq[Setting[_]] = Seq(
    sourcePositionMappers += TemplateProblem.positionMapper(Codec(sourceEncoding.value))
  )

  def dependencySettings = Def.settings(
    // Task is undefined if not a ScalaJSPlugin
    isScalaJSProject := (isScalaJSProject ?? false).value,
    twirlVersion := readResourceProperty("twirl.version.properties", "twirl.api.version"),
    // %%% will be the same as %% for normal projects
    libraryDependencies += "com.typesafe.play" %%% "twirl-api" % twirlVersion.value
  )

  def scalacEncoding(options: Seq[String]): String = {
    val i = options.indexOf("-encoding") + 1
    if (i > 0 && i < options.length) options(i) else "UTF-8"
  }

  def defaultFormats = Map(
    "html" -> "play.twirl.api.HtmlFormat",
    "txt" -> "play.twirl.api.TxtFormat",
    "xml" -> "play.twirl.api.XmlFormat",
    "js" -> "play.twirl.api.JavaScriptFormat"
  )

  def compileTemplatesTask = Def.task {
    TemplateCompiler.compile(
      (sourceDirectories in compileTemplates).value,
      (target in compileTemplates).value,
      templateFormats.value,
      templateImports.value,
      constructorAnnotations.value,
      (includeFilter in compileTemplates).value,
      (excludeFilter in compileTemplates).value,
      Codec(sourceEncoding.value),
      streams.value.log
    )
  }

  def readResourceProperty(resource: String, property: String): String = {
    val props = new java.util.Properties
    val stream = getClass.getClassLoader.getResourceAsStream(resource)
    try {
      props.load(stream)
    }
    catch {
      case e: Exception =>
    }
    finally {
      if (stream ne null) stream.close
    }
    props.getProperty(property)
  }
}
