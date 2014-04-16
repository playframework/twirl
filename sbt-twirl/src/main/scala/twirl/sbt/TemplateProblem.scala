/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.sbt

import sbt._
import twirl.compiler.{ GeneratedSource, MaybeGeneratedSource }
import xsbti.{ CompileFailed, Maybe, Position, Problem, Severity }

object TemplateProblem {

  val positionMapper: Position => Option[Position] = position => {
    position.sourceFile collect {
      case MaybeGeneratedSource(generated) => TemplatePosition(generated, position)
    }
  }

  def exception(source: File, message: String, line: Int, column: Int) = {
    new ProblemException(TemplateProblem(message, TemplatePosition(source, line, column)))
  }

  class ProblemException(issues: Problem*) extends CompileFailed with FeedbackProvidedException {
    def arguments(): Array[String] = Array.empty
    def problems(): Array[Problem] = issues.toArray
    override def toString = "Twirl compilation failed"
  }

  case class TemplateProblem(message: String, position: Position) extends Problem {
    def category: String = "undefined"
    def severity: Severity = Severity.Error
  }

  object TemplatePosition {
    def apply(source: File, line: Int, column: Int): TemplatePosition = {
      val lines = IO.readLines(source)
      val offset = offsetBefore(lines, line) + column
      val mapping = TemplateMapping(line, column, offset, lines(line - 1))
      new TemplatePosition(Some(source), mapping)
    }

    def apply(generated: GeneratedSource, position: Position): TemplatePosition = {
      val line = position.line map { l => generated.mapLine(l) } getOrElse 0
      val lines = generated.source.toSeq flatMap { file => IO.readLines(file) }
      val offset = position.offset map { o => generated.mapPosition(o) } getOrElse 0
      val column = offset - offsetBefore(lines, line)
      val mapping = TemplateMapping(line, column, offset, lines(line - 1))
      new TemplatePosition(generated.source, mapping)
    }

    def offsetBefore(lines: Seq[String], line: Int) = {
      lines.take(line - 1).map(_.size + 1).sum
    }
  }

  class TemplatePosition(source: Option[File], mapping: TemplateMapping) extends Position {
    val line: Maybe[Integer] = Maybe.just(mapping.line)

    val lineContent: String = mapping.content

    val offset: Maybe[Integer] = Maybe.just(mapping.offset)

    val pointer: Maybe[Integer] = Maybe.just(mapping.column)

    val pointerSpace: Maybe[String] = Maybe.just {
      lineContent.take(mapping.column) map { case '\t' => '\t'; case _ => ' ' }
    }

    val sourceFile: Maybe[File] = maybe(source)

    val sourcePath: Maybe[String] = maybe {
      source map (_.getCanonicalPath)
    }
  }

  case class TemplateMapping(line: Int, column: Int, offset: Int, content: String)

  def maybe[A](o: Option[A]): Maybe[A] = o match {
    case Some(v) => Maybe.just(v)
    case None => Maybe.nothing()
  }
}
