/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.sbt

import sbt._
import play.twirl.compiler._
import scala.io.Codec
import sbt.internal.inc.LoggedReporter

object TemplateCompiler {

  def compile(
      sourceDirectories: Seq[File],
      targetDirectory: File,
      templateFormats: Map[String, String],
      templateImports: Seq[String],
      constructorAnnotations: Seq[String],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      codec: Codec,
      log: Logger,
      scalaVersion: String
  ): Seq[File] = {
    try {
      syncGenerated(targetDirectory, codec)
      val templates = collectTemplates(sourceDirectories, templateFormats, includeFilter, excludeFilter)
      for ((template, sourceDirectory, extension, format) <- templates) {
        val imports = TwirlCompiler.formatImports(templateImports, extension)
        TwirlCompiler.compile(
          template,
          sourceDirectory,
          targetDirectory,
          format,
          Some(scalaVersion),
          imports,
          constructorAnnotations,
          codec,
          inclusiveDot = false
        )
      }
      generatedFiles(targetDirectory).map(_.getAbsoluteFile)
    } catch handleError(log, codec)
  }

  private def handleError(log: Logger, codec: Codec): PartialFunction[Throwable, Nothing] = {
    case TemplateCompilationError(source, message, line, column) =>
      val exception = TemplateProblem.exception(source, codec, message, line, column)
      val reporter  = new LoggedReporter(10, log)
      exception.problems.foreach { p => reporter.log(p) }
      throw exception
    case e => throw e
  }

  def generatedFiles(targetDirectory: File): Seq[File] = {
    (targetDirectory ** "*.template.scala").get
  }

  def syncGenerated(targetDirectory: File, codec: Codec): Unit = {
    generatedFiles(targetDirectory).map(GeneratedSource(_, codec)).foreach(_.sync)
  }

  def collectTemplates(
      sourceDirectories: Seq[File],
      templateFormats: Map[String, String],
      includeFilter: FileFilter,
      excludeFilter: FileFilter
  ): Seq[(File, File, String, String)] = {
    sourceDirectories.flatMap { sourceDirectory =>
      (sourceDirectory ** includeFilter).get.flatMap { file =>
        val ext = file.name.split('.').last
        if (!excludeFilter.accept(file) && templateFormats.contains(ext))
          Some((file, sourceDirectory, ext, templateFormats(ext)))
        else
          None
      }
    }
  }

}
