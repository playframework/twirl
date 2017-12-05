/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.parser
package test

import org.scalatest.{Inside, MustMatchers, WordSpec}
import play.twirl.parser.TreeNodes._

class ParserSpec extends WordSpec with MustMatchers with Inside {

  val parser = new TwirlParser(shouldParseInclusiveDot = false)

  def get(templateName: String): String = {
    TwirlIO.readUrlAsString(this.getClass.getClassLoader.getResource(templateName))
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

  def parseStringSuccess(template: String) = parseString(template) must matchPattern {
    case parser.Success(_, rest) if rest.atEnd() =>
  }

  def parseFailure(templateName: String, message: String, line: Int, column: Int) = inside(parse(templateName)) {
    case parser.Error(_, rest, errors) =>
      val e = errors.head
      e.str mustBe message
      e.pos.line mustBe line
      e.pos.column mustBe column
  }

  def parseTemplate(templateName: String): Template = {
    parseTemplateString(get(templateName))
  }

  def parseTemplateString(template: String): Template = {
    parser.parse(template) match {
      case parser.Success(tmpl, input) =>
        if (!input.atEnd) sys.error("Template parsed but not at source end")
        tmpl
      case parser.Error(_, _, errors) =>
        sys.error("Template failed to parse: " + errors.head.str)
    }
  }

  "New twirl parser" should {

    "succeed for" when {

      "static.scala.html" in {
        parseSuccess("static.scala.html")
      }

      "simple.scala.html" in {
        parseSuccess("simple.scala.html")
      }

      "complicated.scala.html" in {
        parseSuccess("complicated.scala.html")
      }

      "pattern matching" in {
        parseSuccess("patternMatching.scala.html")
      }

      "elseIf.scala.html" in {
        val template = parseTemplate("elseIf.scala.html")
        val node = template.content(1)
        val expressions = node.asInstanceOf[Display].exp.parts
        expressions.head must be (Simple("if(input == 5)"))
        expressions(1).asInstanceOf[Block]
        expressions(2) must be (Simple("else if(input == 6)"))
        expressions(3).asInstanceOf[Block]
        expressions(4) must be (Simple("else if(input == 8)"))
        expressions(5).asInstanceOf[Block]
        expressions(6) must be (Simple("else"))
      }

      "imports.scala.html" in {
        parseTemplate("imports.scala.html").topImports must be (Seq(
          Simple("import java.io.File"),
          Simple("import java.net.URL")
        ))
      }

      "case.scala.js" in {
        parseSuccess("case.scala.js")
      }

      "JavaScript case and arrow functions" in {
        parseSuccess("caseAndArrowFunctions.scala.js")
      }

      "import expressions" in {
        parseTemplateString("@import identifier").topImports must be (Seq(Simple("import identifier")))
        parseTemplateString("@importIdentifier").topImports mustBe empty
      }

    }

    "handle string literals within parentheses" when {

      "with left parenthesis" in {
        parseStringSuccess("""@foo("(")""")
      }

      "with right parenthesis and '@'" in {
        parseStringSuccess("""@foo(")@")""")
      }

    }

    "handle escaped closing curly braces" in {
      parseStringSuccess("""@for(i <- is) { @} }""")
    }

    "fail for" when {

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

