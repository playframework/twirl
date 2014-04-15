/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.sbt

import sbt._
import xsbti.{ Maybe, Position }

object PositionMapper {
  val templates: Position => Option[Position] = position => {
    position.sourceFile collect {
      case twirl.compiler.MaybeGeneratedSource(generatedSource) => new Position {

        lazy val line: Maybe[Integer] = maybe {
          position.line map { l => generatedSource.mapLine(l) }
        }

        lazy val lineContent: String = {
          line flatMap { ln => sourceLines.lift(ln - 1) } getOrElse ""
        }

        val offset: Maybe[Integer] = Maybe.nothing()

        lazy val pointer: Maybe[Integer] = maybe {
          for {
            offset <- position.offset
            mapped = generatedSource.mapPosition(offset)
            ln <- line
            prefix = sourceLines.take(ln - 1).map(_.size + 1).sum
          } yield {
            mapped - prefix
          }
        }

        lazy val pointerSpace: Maybe[String] = maybe {
          pointer map { p =>
            lineContent.take(p) map { case '\t' => '\t'; case _ => ' ' }
          }
        }

        val sourceFile: Maybe[File] = maybe {
          generatedSource.source
        }

        val sourcePath: Maybe[String] = maybe {
          generatedSource.source map (_.getCanonicalPath)
        }

        private lazy val sourceLines: Seq[String] = {
          generatedSource.source.toSeq flatMap { file => IO.readLines(file) }
        }

      }
    }
  }

  def maybe[A](o: Option[A]): Maybe[A] = o match {
    case Some(v) => Maybe.just(v)
    case None => Maybe.nothing()
  }
}
