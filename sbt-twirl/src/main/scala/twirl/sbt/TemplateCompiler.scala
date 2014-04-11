/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.sbt

import java.io.File
import sbt._
import twirl.compiler._

object TemplateCompiler {
  def compile(sourceDirectories: Seq[File], targetDirectory: File, templateFormats: Map[String, String], templateImports: Seq[String], includeFilter: FileFilter, excludeFilter: FileFilter) = {
    try {
      syncGenerated(targetDirectory)
      val templates = collectTemplates(sourceDirectories, templateFormats, includeFilter, excludeFilter)
      for ((template, sourceDirectory, extension, format) <- templates) {
        val imports = formatImports(templateImports, extension)
        TwirlCompiler.compile(template, sourceDirectory, targetDirectory, format, imports)
      }
      generatedFiles(targetDirectory).map(_.getAbsoluteFile)
    } catch handleError
  }

  def generatedFiles(targetDirectory: File): Seq[File] = {
    (targetDirectory ** "*.template.scala").get
  }

  def syncGenerated(targetDirectory: File): Unit = {
    generatedFiles(targetDirectory).map(GeneratedSource).foreach(_.sync)
  }

  def collectTemplates(sourceDirectories: Seq[File], templateFormats: Map[String, String], includeFilter: FileFilter, excludeFilter: FileFilter): Seq[(File, File, String, String)] = {
    sourceDirectories flatMap { sourceDirectory =>
      (sourceDirectory ** includeFilter).get flatMap { file =>
        val ext = file.name.split('.').last
        if (!excludeFilter.accept(file) && templateFormats.contains(ext))
          Some((file, sourceDirectory, ext, templateFormats(ext)))
        else
          None
      }
    }
  }

  def formatImports(templateImports: Seq[String], extension: String): String = {
    templateImports.map("import " + _.replace("%format%", extension)).mkString("\n")
  }

  val handleError: PartialFunction[Throwable, Nothing] = {
    case TemplateCompilationError(source, message, line, column) =>
      throw new Exception("TODO: problem reporting")
    case e => throw e
  }
}
