/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.parser
package test

import org.specs2.mutable._
import play.twirl.parser.TreeNodes.{ Simple, Template }

object OldParserSpec extends Specification {

  val parser = new PlayTwirlParser

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

  def parseStringSuccess(template: String) = parseString(template) must beLike {
    case parser.Success(_, rest) if rest.atEnd => ok
  }

  def parseFailure(templateName: String, message: String, line: Int, column: Int) = parse(templateName) must beLike {
    case parser.NoSuccess(msg, rest) => {
      if (msg == message && rest.pos.line == line && rest.pos.column == column) ok else ko
    }
  }

  def parseTemplate(templateName: String): Template = {
    parseTemplateString(get(templateName))
  }

  def parseTemplateString(template: String): Template = {
    parseString(template) match {
      case parser.Success(template, rest) =>
        if (!rest.atEnd) sys.error("Template parsed but not at source end")
        template
      case parser.NoSuccess(msg, _) =>
        sys.error("Template failed to parse: " + msg)
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

      "imports.scala.html" in {
        parseTemplate("imports.scala.html").topImports must be_== (Seq(
          Simple("import java.io.File\n"),
          Simple("import java.net.URL\n")
        ))
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
