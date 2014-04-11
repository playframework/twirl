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
    dependencySettings

  def twirlSettings: Seq[Setting[_]] = Seq(
    templateFormats := defaultFormats,
    templateImports := Seq.empty,
    sourceDirectories in compileTemplates := Seq(sourceDirectory.value / "twirl"),
    target in compileTemplates := crossTarget.value / "twirl" / Defaults.nameForSrc(configuration.value.name),
    excludeFilter in compileTemplates := HiddenFileFilter,
    compileTemplates := compileTemplatesTask.value,
    sourceGenerators <+= compileTemplates,
    managedSourceDirectories <+= target in compileTemplates
  )

  def dependencySettings: Seq[Setting[_]] = Seq(
    twirlVersion := "1.0-SNAPSHOT", // TODO: read from properties file
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
      (excludeFilter in compileTemplates).value
    )
  }
}
