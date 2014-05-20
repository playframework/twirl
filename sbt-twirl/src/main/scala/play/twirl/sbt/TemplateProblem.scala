/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.sbt

import sbt._
import play.twirl.compiler.{ GeneratedSource, MaybeGeneratedSource }
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
      val location = TemplateMapping(Some(source)).location(line, column)
      new TemplatePosition(Some(source), location)
    }

    def apply(generated: GeneratedSource, position: Position): TemplatePosition = {
      val offset = position.offset map { o => generated.mapPosition(o) }
      val location = offset flatMap { o => TemplateMapping(generated.source).location(o) }
      new TemplatePosition(generated.source, location)
    }
  }

  class TemplatePosition(source: Option[File], location: Option[TemplateMapping.Location]) extends Position {
    val line: Maybe[Integer] = maybe { location map (_.line) }

    val lineContent: String = location.fold("")(_.content)

    val offset: Maybe[Integer] = maybe { location map (_.offset) }

    val pointer: Maybe[Integer] = maybe { location map (_.column) }

    val pointerSpace: Maybe[String] = maybe {
      location.map { l => lineContent.take(l.column) map { case '\t' => '\t'; case _ => ' ' } }
    }

    val sourceFile: Maybe[File] = maybe(source)

    val sourcePath: Maybe[String] = maybe { source map (_.getCanonicalPath) }
  }

  object TemplateMapping {
    case class Location(line: Int, column: Int, offset: Int, content: String)

    case class Line(line: Int, start: Int, end: Int, content: String) {
      def location(l: Int, c: Int): Location = {
        if (l < line) {
          Location(line, 0, start, content)
        } else if (l > line) {
          Location(line, content.length, end, content)
        } else {
          val column = 0 max c min content.length
          val offset = start + column
          Location(line, column, offset, content)
        }
      }

      def location(o: Int): Location = {
        val offset = start max o min end
        val column = offset - start
        Location(line, column, offset, content)
      }
    }

    def apply(source: Option[File]): TemplateMapping = {
      val lines = source.toSeq flatMap { file => IO.readLines(file) }
      TemplateMapping(lines)
    }
  }

  case class TemplateMapping(sourceLines: Seq[String]) {
    import TemplateMapping.{ Line, Location }

    val lines: Seq[Line] = sourceLines.scanLeft(Line(0, -1, -1, "")) {
      (previous, content) => Line(previous.line + 1, previous.end + 1, previous.end + 1 + content.length, content)
    }.drop(1)

    def location(offset: Int): Option[Location] = {
      if (lines.isEmpty) {
        None
      } else {
        val index = 0 max lines.lastIndexWhere(_.start <= offset)
        Some(lines(index).location(offset))
      }
    }

    def location(line: Int, column: Int): Option[Location] = {
      if (lines.isEmpty) {
        None
      } else {
        val index = 0 max (line - 1) min (lines.length - 1)
        Some(lines(index).location(line, column))
      }
    }
  }

  def maybe[A](o: Option[A]): Maybe[A] = o match {
    case Some(v) => Maybe.just(v)
    case None => Maybe.nothing()
  }
}
