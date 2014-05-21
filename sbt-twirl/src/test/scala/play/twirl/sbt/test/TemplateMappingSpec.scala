/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.sbt
package test

import org.specs2.mutable._
import play.twirl.sbt.TemplateProblem.TemplateMapping
import play.twirl.sbt.TemplateProblem.TemplateMapping.Location

class TemplateMappingSpec extends Specification {

  "TemplateMapping" should {

    "handle empty templates" in {
      val mapping = TemplateMapping(Seq.empty)

      mapping.location(offset = 0) must beNone
      mapping.location(offset = 7) must beNone

      mapping.location(line = 1, column = 0) must beNone
      mapping.location(line = 7, column = 7) must beNone
    }

    "map positions from offset or (line, column)" in {
      val mapping = TemplateMapping(Seq("ab", "c", "", "d"))

      val testLocations = Seq(
        Location(1, 0, 0, "ab"),
        Location(1, 2, 2, "ab"),
        Location(2, 1, 4, "c"),
        Location(3, 0, 5, ""),
        Location(4, 1, 7, "d")
      )

      forall(testLocations) { location =>
        mapping.location(location.offset) must beSome(location)
        mapping.location(location.line, location.column) must beSome(location)
      }
    }

    "map invalid positions to nearest location" in {
      val mapping = TemplateMapping(Seq("ab", "c", "", "d"))

      val testOffsets = Seq(
        -1 -> Location(1, 0, 0, "ab"),
        10 -> Location(4, 1, 7, "d")
      )

      forall(testOffsets) { case (offset, location) =>
        mapping.location(offset) must beSome(location)
      }

      val testPositions = Seq(
        (0, 7)  -> Location(1, 0, 0, "ab"),
        (1, -1) -> Location(1, 0, 0, "ab"),
        (1, 7)  -> Location(1, 2, 2, "ab"),
        (3, 7)  -> Location(3, 0, 5, ""),
        (4, 7)  -> Location(4, 1, 7, "d"),
        (5, -1) -> Location(4, 1, 7, "d"),
        (5, 0)  -> Location(4, 1, 7, "d")
      )

      forall(testPositions) { case ((line, column), location) =>
        mapping.location(line, column) must beSome(location)
      }
    }
  }
}
