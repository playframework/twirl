/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.sbt.test

import java.io.File

import play.twirl.sbt.TemplateProblem.{TemplateMapping, TemplatePosition}
import org.scalatest.{Inspectors, MustMatchers, WordSpec}

class TemplatePositionSpec extends WordSpec with MustMatchers with Inspectors {

  "TemplatePosition" should {

    "toString" should {

      val sourceFile = new File("/some/path/file.scala.html")
      val sourceAbsolutePath = sourceFile.getAbsolutePath // NOTE: Computed like this to work cross-platform (Linux and Windows)

      "have the source path" in {
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(Option(sourceFile), Option(location))

        tp.toString mustBe sourceAbsolutePath + ":10\nsome content"
      }

      "not have the source path when it is empty" in {
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(None, Option(location))

        tp.toString mustNot include(sourceAbsolutePath)
      }

      "have line if present" in {
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(Option(sourceFile), Option(location))

        tp.toString must include("10")
      }

      "not have line when it is empty" in {
        val tp = new TemplatePosition(Option(sourceFile), None /* means no location for the error, then no line */)

        tp.toString mustBe sourceAbsolutePath
      }

      "have line content if present" in {
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(Option(sourceFile), Option(location))

        tp.toString must include("some content")
      }

      "not have line content when it is missing" in {
        val tp = new TemplatePosition(Option(sourceFile), None /* means no location for the error, then no offset */)

        tp.toString mustBe sourceAbsolutePath
      }

      "not have line content when it is empty" in {
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "")
        val tp = new TemplatePosition(Option(sourceFile), Option(location))

        tp.toString mustBe sourceAbsolutePath + ":10"
      }
    }
  }

}
