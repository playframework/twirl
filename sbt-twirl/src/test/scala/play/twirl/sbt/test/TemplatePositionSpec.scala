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

      "have the source path" in {
        val file = new File("/some/path/file.scala.html")
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(Option(file), Option(location))

        tp.toString mustBe "/some/path/file.scala.html:10:22"
      }

      "not have the source path when it is empty" in {
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(None, Option(location))

        tp.toString mustNot include("/some/path/file.scala.html")
      }

      "have line if present" in {
        val file = new File("/some/path/file.scala.html")
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(Option(file), Option(location))

        tp.toString must include("10")
      }

      "not have line when it is empty" in {
        val file = new File("/some/path/file.scala.html")
        val tp = new TemplatePosition(Option(file), None /* means no location for the error, then no line */)

        tp.toString mustBe "/some/path/file.scala.html"
      }

      "have offset if present" in {
        val file = new File("/some/path/file.scala.html")
        val location = TemplateMapping.Location(line = 10, column = 2, offset = 22, content = "some content")
        val tp = new TemplatePosition(Option(file), Option(location))

        tp.toString must include("22")
      }

      "not have offset when it is empty" in {
        val file = new File("/some/path/file.scala.html")
        val tp = new TemplatePosition(Option(file), None /* means no location for the error, then no offset */)

        tp.toString mustBe "/some/path/file.scala.html"
      }
    }
  }

}
