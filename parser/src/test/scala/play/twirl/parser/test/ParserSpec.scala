/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.parser
package test

import org.specs2.mutable._
import play.twirl.parser.TreeNodes.{ Simple, Template }

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
      (e.str must_== message) and (e.pos.line must_== line) and (e.pos.column must_== column)
    }
  }

  def parseTemplate(templateName: String): Template = {
    parseTemplateString(get(templateName))
  }

  def parseTemplateString(template: String): Template = {
    val parser = new TwirlParser(shouldParseInclusiveDot = false)
    parser.parse(template) match {
      case parser.Success(template, input) =>
        if (!input.atEnd) sys.error("Template parsed but not at source end")
        template
      case parser.Error(_, _, errors) =>
        sys.error("Template failed to parse: " + errors.head.str)
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

      "imports.scala.html" in {
        parseTemplate("imports.scala.html").topImports must be_== (Seq(
          Simple("import java.io.File"),
          Simple("import java.net.URL")
        ))
      }

      "case.scala.js" in {
        parseSuccess("case.scala.js")
      }

      "import expressions" in {
        parseTemplateString("@import identifier").topImports must be_== (Seq(Simple("import identifier")))
        parseTemplateString("@importIdentifier").topImports must beEmpty
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
        parseFailure("unclosedBracket.scala.html", "Expected '}' but found 'EOF'", 12, 6)
      }

      "unclosedBracket2.scala.html" in {
        parseFailure("unclosedBracket2.scala.html", "Expected '}' but found 'EOF'", 32, 1)
      }

      "invalidAt.scala.html" in {
        parseFailure("invalidAt.scala.html", "Invalid '@' symbol", 5, 5)
      }

    }

  }

}
