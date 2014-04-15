/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.sbt

import sbt._
import xsbti.{ CompileFailed, Maybe, Position, Problem, Severity }

object TemplateProblem {

  def exception(source: File, message: String, line: Int, column: Int) = {
    new ProblemException(TemplateProblem(message, new TemplatePosition(source, line, column)))
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

  class TemplatePosition(source: File, lineNo: Int, column: Int) extends Position {
    lazy val line: Maybe[Integer] = Maybe.just(lineNo)

    lazy val lineContent: String = {
      line flatMap { ln => sourceLines.lift(ln - 1) } getOrElse ""
    }

    val offset: Maybe[Integer] = Maybe.nothing()

    lazy val pointer: Maybe[Integer] = Maybe.just(column)

    lazy val pointerSpace: Maybe[String] = maybe {
      pointer map { p =>
        lineContent.take(p) map { case '\t' => '\t'; case _ => ' ' }
      }
    }

    val sourceFile: Maybe[File] = Maybe.just(source)

    val sourcePath: Maybe[String] = Maybe.just(source.getCanonicalPath)

    private lazy val sourceLines: Seq[String] = IO.readLines(source)
  }

  def maybe[A](o: Option[A]): Maybe[A] = o match {
    case Some(v) => Maybe.just(v)
    case None => Maybe.nothing()
  }
}
