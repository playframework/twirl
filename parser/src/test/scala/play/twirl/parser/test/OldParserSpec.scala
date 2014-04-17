/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.parser
package test

import org.specs2.mutable._
import scalax.io.Resource

object OldParserSpec extends Specification {

  val parser = new PlayTwirlParser

  def get(templateName: String): String = {
    Resource.fromClasspath(templateName, this.getClass).string
  }

  def parse(templateName: String) = {
    parseString(get(templateName))
  }

  def parseString(template: String) = {
    parser.parse(template)
  }

  def parseSuccess(templateName: String) = {
    parseStringSuccess(get(templateName))
  }

  def parseStringSuccess(template: String) = parseString(template) must beLike {
    case parser.Success(_, rest) if rest.atEnd => ok
  }

  def parseFailure(templateName: String, message: String, line: Int, column: Int) = parse(templateName) must beLike {
    case parser.NoSuccess(msg, rest) => {
      if (msg == message && rest.pos.line == line && rest.pos.column == column) ok else ko
    }
  }

  "Old twirl parser" should {

    "succeed for" in {

      "static.scala.html" in {
        parseSuccess("static.scala.html")
      }

      "simple.scala.html" in {
        parseSuccess("simple.scala.html")
      }

      "complicated.scala.html" in {
        parseSuccess("complicated.scala.html")
      }

    }

    "handle parentheses in string literals" in {

      "with left parenthesis" in {
        parseStringSuccess("""@foo("(")""")
      }

      "with right parenthesis and '@'" in {
        parseStringSuccess("""@foo(")@")""")
      }

    }

    "fail for" in {

      "unclosedBracket.scala.html" in {
        parseFailure("unclosedBracket.scala.html", "Unmatched bracket", 8, 12)
      }

      "unclosedBracket2.scala.html" in {
        parseFailure("unclosedBracket2.scala.html", "Unmatched bracket", 13, 20)
      }

      "invalidAt.scala.html" in {
        parseFailure("invalidAt.scala.html", "`identifier' expected but `<' found", 5, 6)
      }

    }

  }

}
