/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.parser
package test

import org.specs2.mutable._

object ParserSpec extends Specification {

  val parser = new TwirlParser(shouldParseInclusiveDot = false)

  def get(templateName: String): String = {
    TwirlIO.readUrlAsString(this.getClass.getClassLoader.getResource(templateName))
  }

  def parse(templateName: String) = {
    parseString(get(templateName))
  }

  def parseString(template: String) = {
    (new TwirlParser(shouldParseInclusiveDot = false)).parse(template)
  }

  def parseSuccess(templateName: String) = {
    parseStringSuccess(get(templateName))
  }

  def parseStringSuccess(template: String) = parseString(template) must beLike {
    case parser.Success(_, rest) if rest.atEnd => ok
  }

  def parseFailure(templateName: String, message: String, line: Int, column: Int) = parse(templateName) must beLike {
    case parser.Error(_, rest, errors) => {
      val e = errors.head
      if (e.str == message && e.pos.line == line && e.pos.column == column) ok else ko
    }
  }

  "New twirl parser" should {

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

    "handle string literals within parentheses" in {

      "with left parenthesis" in {
        parseStringSuccess("""@foo("(")""")
      }

      "with right parenthesis and '@'" in {
        parseStringSuccess("""@foo(")@")""")
      }

    }

    "fail for" in {

      "unclosedBracket.scala.html" in {
        parseFailure("unclosedBracket.scala.html", "[ERROR] Expected '}' but found: 'EOF'.", 12, 6)
      }

      "unclosedBracket2.scala.html" in {
        parseFailure("unclosedBracket2.scala.html", "[ERROR] Expected '}' but found: 'EOF'.", 32, 1)
      }

      "invalidAt.scala.html" in {
        parseFailure("invalidAt.scala.html", "[ERROR] Invalid '@' symbol.", 5, 6)
      }

    }

  }

}
