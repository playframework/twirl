/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.sbt

import sbt._
import sbt.Keys._

object Import {
  object TwirlKeys {
    val twirlVersion = SettingKey[String]("twirl-version", "Twirl version used for twirl-api dependency")
    val templateFormats = SettingKey[Map[String, String]]("twirl-template-formats", "Defined twirl template formats")
    val templateImports = SettingKey[Seq[String]]("twirl-template-imports", "Extra imports for twirl templates")
    val compileTemplates = TaskKey[Seq[File]]("twirl-compile-templates", "Compile twirl templates into scala source files")
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
    positionSettings ++
    dependencySettings

  def twirlSettings: Seq[Setting[_]] = Seq(
    templateFormats := defaultFormats,
    templateImports := Seq.empty,

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

  def positionSettings: Seq[Setting[_]] = Seq(
    sourcePositionMappers += TemplateProblem.positionMapper
  )

  def dependencySettings: Seq[Setting[_]] = Seq(
    twirlVersion := readResourceProperty("twirl.version.properties", "twirl.api.version"),
    libraryDependencies += "com.typesafe.twirl" %% "twirl-api" % twirlVersion.value
  )

  def defaultFormats = Map(
    "html" -> "twirl.api.HtmlFormat",
    "txt" -> "twirl.api.TxtFormat",
    "xml" -> "twirl.api.XmlFormat",
    "js" -> "twirl.api.JavaScriptFormat"
  )

  def compileTemplatesTask = Def.task {
    TemplateCompiler.compile(
      (sourceDirectories in compileTemplates).value,
      (target in compileTemplates).value,
      templateFormats.value,
      templateImports.value,
      (includeFilter in compileTemplates).value,
      (excludeFilter in compileTemplates).value,
      streams.value.log
    )
  }

  def readResourceProperty(resource: String, property: String): String = {
    val props = new java.util.Properties
    val stream = getClass.getClassLoader.getResourceAsStream(resource)
    try { props.load(stream) }
    catch { case e: Exception => }
    finally { if (stream ne null) stream.close }
    props.getProperty(property)
  }
}
