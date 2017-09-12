/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.sbt

import play.twirl.compiler.TemplateCompilationError
import sbt.Logger
import sbt.internal.inc.LoggedReporter

import scala.io.Codec

trait TemplateCompilerErrorHandler {

  def handleError(log: Logger, codec: Codec): PartialFunction[Throwable, Nothing] = {
    case TemplateCompilationError(source, message, line, column) =>
      val exception = TemplateProblem.exception(source, codec, message, line, column)
      val reporter = new LoggedReporter(10, log)
      exception.problems foreach { p => reporter.log(p) }
      throw exception
    case e => throw e
  }
}
