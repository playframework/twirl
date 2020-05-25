/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.parser
package test

import org.scalatest.Inside
import play.twirl.parser.TreeNodes._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParserSpec extends AnyWordSpec with Matchers with Inside {

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

  def parseStringSuccess(template: String) =
    parseString(template) must matchPattern {
      case parser.Success(_, rest) if rest.atEnd() =>
    }

  def parseFailure(templateName: String, message: String, line: Int, column: Int) =
    inside(parse(templateName)) {
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

      "elseIf.scala.html" in {
        val template    = parseTemplate("elseIf.scala.html")
        val node        = template.content(1)
        val expressions = node.asInstanceOf[Display].exp.parts
        expressions.head must be(Simple("if(input == 5)"))
        expressions(1).asInstanceOf[Block]
        expressions(2) must be(Simple("else if(input == 6)"))
        expressions(3).asInstanceOf[Block]
        expressions(4) must be(Simple("else if(input == 8)"))
        expressions(5).asInstanceOf[Block]
        expressions(6) must be(Simple("else"))
      }

      "imports.scala.html" in {
        parseTemplate("imports.scala.html").topImports must be(
          Seq(
            Simple("import java.io.File"),
            Simple("import java.net.URL")
          )
        )
      }

      "case.scala.js" in {
        parseSuccess("case.scala.js")
      }

      "import expressions" in {
        parseTemplateString("@import identifier").topImports must be(Seq(Simple("import identifier")))
        parseTemplateString("@importIdentifier").topImports mustBe empty
      }

      "code block containing => of another statement with curly braces in first line" in {
        val tmpl =
          parseTemplateString(
            """@if(attrs!=null){@attrs.map{ v => @v._1 }}"""
          ) // "@attrs.map{ v =>" should not be handled as block args
        val ifExpressions = tmpl.content(0).asInstanceOf[Display].exp.parts
        ifExpressions.head must be(Simple("if(attrs!=null)"))
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].content(0).asInstanceOf[Display].exp.parts
        ifBlockBody.head must be(Simple("attrs"))
        ifBlockBody(1) must be(Simple(".map"))
        val mapBlock = ifBlockBody(2).asInstanceOf[Block]
        mapBlock.args.map(_.toString) mustBe Some(" v =>")
        val mapBlockBody = ifBlockBody(2).asInstanceOf[Block].content(1).asInstanceOf[Display].exp.parts
        mapBlockBody.head must be(Simple("v"))
        mapBlockBody(1) must be(Simple("._1"))
      }

      "code block containing => of another statement with parentheses in first line" in {
        val tmpl =
          parseTemplateString(
            """@if(attrs!=null){@attrs.map( v => @v._1 )}"""
          ) // "@attrs.map( v =>" should not be handled as block args
        val ifExpressions = tmpl.content(0).asInstanceOf[Display].exp.parts
        ifExpressions.head must be(Simple("if(attrs!=null)"))
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].content(0).asInstanceOf[Display].exp.parts
        ifBlockBody.head must be(Simple("attrs"))
        ifBlockBody(1) must be(Simple(".map( v => @v._1 )"))
      }

      "code block containing (...) => in first line" in {
        val tmpl =
          parseTemplateString(
            """@if(attrs!=null){( arg1, arg2 ) => @arg1.toString }"""
          ) // "( arg1, arg2 ) =>" should be handled as block args
        val ifExpressions = tmpl.content(0).asInstanceOf[Display].exp.parts
        ifExpressions.head must be(Simple("if(attrs!=null)"))
        val ifBlock = ifExpressions(1).asInstanceOf[Block]
        ifBlock.args.map(_.toString) mustBe Some("( arg1, arg2 ) =>")
        val ifBlockBody = ifBlock.content(1).asInstanceOf[Display].exp.parts
        ifBlockBody.head must be(Simple("arg1"))
        ifBlockBody(1) must be(Simple(".toString"))
      }

      "text outside of code block on same line containing =>" in {
        val tmpl =
          parseTemplateString(
            """@if(attrs!=null){blockbody}Some plain text with => inside"""
          ) // "blockbody}Some plain text with =>" should not be handled as block args
        val ifExpressions = tmpl.content(0).asInstanceOf[Display].exp.parts
        ifExpressions.head must be(Simple("if(attrs!=null)"))
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].content(0).asInstanceOf[Plain]
        ifBlockBody.text mustBe "blockbody"
        val outsideIf = tmpl.content(1).asInstanceOf[Plain]
        outsideIf.text mustBe "Some plain text with => inside"
      }

      "match statement not allowed to have block arguments" in {
        val tmpl = parseTemplateString(
          """@fooVariable match { case x: String => { Nice string } case _ => { Not a nice string } }"""
        ) // " case x: String =>" should not be handled as block args of the match block
        val matchExpressions = tmpl.content(0).asInstanceOf[Display].exp.parts
        matchExpressions.head must be(Simple("fooVariable"))
        matchExpressions(1) must be(Simple(" match"))

        val matchBlock = matchExpressions(2).asInstanceOf[Block].content

        val firstCaseBlock = matchBlock.head.asInstanceOf[ScalaExp].parts
        firstCaseBlock.head must be(Simple("case x: String =>"))
        val firstCaseBlockBody = firstCaseBlock(1).asInstanceOf[Block]
        firstCaseBlockBody.content(1).asInstanceOf[Plain].text mustBe "Nice string "

        val secondCaseBlock = matchBlock(1).asInstanceOf[ScalaExp].parts
        secondCaseBlock.head must be(Simple("case _ =>"))
        val secondCaseBlockBody = secondCaseBlock(1).asInstanceOf[Block]
        secondCaseBlockBody.content(1).asInstanceOf[Plain].text mustBe "Not a nice string "
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
