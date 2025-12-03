/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.parser
package test

import org.scalatest.Inside
import play.twirl.parser.TreeNodes._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.ListBuffer

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
    inside(parse(templateName)) { case parser.Error(_, rest, errors) =>
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
        if (!input.atEnd()) sys.error("Template parsed but not at source end")
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
        ifExpressions(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.content.size must be(1)
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].contents.content(0).asInstanceOf[Display].exp.parts
        ifBlockBody.head must be(Simple("attrs"))
        ifBlockBody(1) must be(Simple(".map"))
        val mapBlock = ifBlockBody(2).asInstanceOf[Block]
        mapBlock.args.map(_.toString) mustBe Some(" v =>")
        ifBlockBody(2).asInstanceOf[Block].contents.imports mustBe empty
        ifBlockBody(2).asInstanceOf[Block].contents.members mustBe empty
        ifBlockBody(2).asInstanceOf[Block].contents.sub mustBe empty
        ifBlockBody(2).asInstanceOf[Block].contents.content.size must be(3)
        val mapBlockBody = ifBlockBody(2).asInstanceOf[Block].contents.content(1).asInstanceOf[Display].exp.parts
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
        ifExpressions(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.content.size must be(1)
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].contents.content(0).asInstanceOf[Display].exp.parts
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
        ifBlock.contents.imports mustBe empty
        ifBlock.contents.members mustBe empty
        ifBlock.contents.sub mustBe empty
        ifBlock.contents.content.size must be(3)
        val ifBlockBody = ifBlock.contents.content(1).asInstanceOf[Display].exp.parts
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
        ifExpressions(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.content.size must be(1)
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].contents.content(0).asInstanceOf[Plain]
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

        matchExpressions(2).asInstanceOf[Block].contents.imports mustBe empty
        matchExpressions(2).asInstanceOf[Block].contents.members mustBe empty
        matchExpressions(2).asInstanceOf[Block].contents.sub mustBe empty
        matchExpressions(2).asInstanceOf[Block].contents.content.size must be(2)

        val matchBlock = matchExpressions(2).asInstanceOf[Block].contents.content

        val firstCaseBlock = matchBlock.head.asInstanceOf[ScalaExp].parts
        firstCaseBlock.head must be(Simple("case x: String =>"))
        val firstCaseBlockBody = firstCaseBlock(1).asInstanceOf[Block]
        firstCaseBlockBody.contents.imports mustBe empty
        firstCaseBlockBody.contents.members mustBe empty
        firstCaseBlockBody.contents.sub mustBe empty
        firstCaseBlockBody.contents.content.size must be(2)
        firstCaseBlockBody.contents.content(1).asInstanceOf[Plain].text mustBe "Nice string "

        val secondCaseBlock = matchBlock(1).asInstanceOf[ScalaExp].parts
        secondCaseBlock.head must be(Simple("case _ =>"))
        val secondCaseBlockBody = secondCaseBlock(1).asInstanceOf[Block]
        secondCaseBlockBody.contents.imports mustBe empty
        secondCaseBlockBody.contents.members mustBe empty
        secondCaseBlockBody.contents.sub mustBe empty
        secondCaseBlockBody.contents.content.size must be(2)
        secondCaseBlockBody.contents.content(1).asInstanceOf[Plain].text mustBe "Not a nice string "
      }

      "whitespaces after 'else {...}' as plain" in {
        val template = parseTemplateString(
          """@if(condition) {ifblock body} else {elseblock body}  Some plain text with whitespaces"""
        )
        val ifExpressions = template.content(0).asInstanceOf[Display].exp.parts
        ifExpressions.head must be(Simple("if(condition)"))
        ifExpressions(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.content.size must be(1)
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].contents.content(0)
        ifBlockBody mustBe Plain("ifblock body")
        val elsePart = ifExpressions(2)
        elsePart mustBe Simple("else")
        ifExpressions(3).asInstanceOf[Block].contents.imports mustBe empty
        ifExpressions(3).asInstanceOf[Block].contents.members mustBe empty
        ifExpressions(3).asInstanceOf[Block].contents.sub mustBe empty
        ifExpressions(3).asInstanceOf[Block].contents.content.size must be(1)
        val elseBlockBody = ifExpressions(3).asInstanceOf[Block].contents.content(0)
        elseBlockBody mustBe Plain("elseblock body")
        val afterIfExpressionOfWhitespaces = template.content(1)
        afterIfExpressionOfWhitespaces mustBe Plain("  ")
        val afterWhitespaces = template.content(2)
        afterWhitespaces mustBe Plain("Some plain text with whitespaces")
      }

      "whitespaces after 'else if(condition) {...}' as plain" in {
        val template = parseTemplateString(
          """@if(condition) {ifblock body} else if(condition2) {elseifblock body}  Some plain text with whitespaces"""
        )
        val ifExpressions = template.content(0).asInstanceOf[Display].exp.parts
        ifExpressions.head must be(Simple("if(condition)"))
        ifExpressions(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpressions(1).asInstanceOf[Block].contents.content.size must be(1)
        val ifBlockBody = ifExpressions(1).asInstanceOf[Block].contents.content(0)
        ifBlockBody mustBe Plain("ifblock body")
        val elseIfPart = ifExpressions(2)
        elseIfPart mustBe Simple("else if(condition2)")
        ifExpressions(3).asInstanceOf[Block].contents.imports mustBe empty
        ifExpressions(3).asInstanceOf[Block].contents.members mustBe empty
        ifExpressions(3).asInstanceOf[Block].contents.sub mustBe empty
        ifExpressions(3).asInstanceOf[Block].contents.content.size must be(1)
        val elseBlockBody = ifExpressions(3).asInstanceOf[Block].contents.content(0)
        elseBlockBody mustBe Plain("elseifblock body")
        val afterIfExpressionOfWhitespaces = template.content(1)
        afterIfExpressionOfWhitespaces mustBe Plain("  ")
        val afterWhitespaces = template.content(2)
        afterWhitespaces mustBe Plain("Some plain text with whitespaces")
      }

      "'if' expression does not have body" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition)"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "'if' expression does not have body but some plain text instead" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition) foo"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "@expression without curly braces or parens as 'if' expression body is currently not supported" in { // might change in future
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition) @someexpr"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "@expression without curly braces or parens as 'if' expression body is currently not supported also when using escaped expression" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition) @@someexpr"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "@(expression) without curly braces or parens as 'if' expression body is currently not supported" in { // might change in future
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition) @(someexpr)"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "@(expression) without curly braces or parens as 'if' expression body is currently not supported also when using escaped expression" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition) @@(someexpr)"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "'if' expression body is invalid when it mimics a method call" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition).callmethod"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "'if' expression body is invalid when it mimics a method call (with space between)" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition) .callmethod"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "'if' expression body is invalid when it only contains one closing bracket" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition)}"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'")
        )
      }

      "'if' expression with pure block body without else body is valid" in {
        val template = parseTemplateString(
          """@if(condition){good}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content.size must be(1)
      }

      "'if' expression with scala block body without else body is valid" in {
        val template = parseTemplateString(
          """@if(condition)@{expr}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts(0) must be(
          Simple("{expr}")
        )
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts.size must be(1)
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content.size must be(1)
      }

      "'if' expression with simple parens body without else body is valid" in {
        val template = parseTemplateString(
          """@if(condition)(expr)"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Simple] must be(Simple("(expr)"))
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content.size must be(1)
      }

      "'else', after an 'if' expression, that does not have a parsable body is be handled as plain text" in {
        val template = parseTemplateString(
          """@if(condition){good}else"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content(1) mustBe Plain("else")
        template.content.size must be(2)
      }

      "'elsewhere', after an 'if' expression, is not handled as 'else' but handled as plain text" in {
        val template = parseTemplateString(
          """@if(condition){good}elsewhere"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content(1) mustBe Plain("elsewhere")
        template.content.size must be(2)
      }

      "@expression without curly braces or parens after 'else' expression is currently not supported" in { // might change in future
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition){good}else @someexpr"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'else'. Hint: To ignore 'else...' and render it as plain string instead you can escape @ with @@."
          )
        )
      }

      "escaped @expression after an 'else' expression makes the whole 'else' part handled as plain text" in {
        val template = parseTemplateString(
          """@if(condition){good}else @@someexpr"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content(1) mustBe Plain("else @someexpr")
        template.content.size must be(2)
      }

      "@(expression) without curly braces or parens after 'else' expression is currently not supported" in { // might change in future
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition){good}else @(someexpr)"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'else'. Hint: To ignore 'else...' and render it as plain string instead you can escape @ with @@."
          )
        )
      }

      "escaped @(expression) after an 'else' expression makes the whole 'else' part handled as plain text" in {
        val template = parseTemplateString(
          """@if(condition){good}else @@(someexpr)"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content(1) mustBe Plain("else @(someexpr)")
        template.content.size must be(2)
      }

      "'else if' expression after an 'if' expression, without body is handled as plain text" in {
        val template = parseTemplateString(
          """@if(condition){good}else if(true)"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content(1) mustBe Plain("else if(true)")
        template.content.size must be(2)
      }

      "@expression without curly braces or parens after 'else if' expression is currently not supported" in { // might change in future
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition){good}else if(true) @someexpr"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'else if(...)'. Hint: To ignore 'else if...' and render it as plain string instead you can escape @ with @@."
          )
        )
      }

      "escaped @expression after an 'else if' expression makes the whole 'else if' part handled as plain text" in {
        val template = parseTemplateString(
          """@if(condition){good}else if(true) @@someexpr"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content(1) mustBe Plain("else if(true) @someexpr")
        template.content.size must be(2)
      }

      "@(expression) without curly braces or parens after 'else if' expression is currently not supported" in { // might change in future
        the[RuntimeException] thrownBy parseTemplateString(
          """@if(condition){good}else if(true) @(someexpr)"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Expected '{ ... }', '@{ ... }' or '(...)' after 'else if(...)'. Hint: To ignore 'else if...' and render it as plain string instead you can escape @ with @@."
          )
        )
      }

      "escaped @(expression) after an 'else if' expression makes the whole 'else if' part handled as plain text" in {
        val template = parseTemplateString(
          """@if(condition){good}else if(true) @@(someexpr)"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content(1) mustBe Plain("else if(true) @(someexpr)")
        template.content.size must be(2)
      }

      "do not render whitespaces which are part of an 'if' expression with {...}" in {
        val template = parseTemplateString(
          """@if(condition)    {good}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if' expression with @{...}" in {
        val template = parseTemplateString(
          """@if(condition)    @{good}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts(0) must be(
          Simple("{good}")
        )
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts.size must be(1)
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if' expression with (...)" in {
        val template = parseTemplateString(
          """@if(condition)    (good)"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Simple] must be(Simple("(good)"))
        ifExpression(2).asInstanceOf[Simple] must be(Simple(" else {null} "))
        ifExpression.size must be(3)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if'/'else' expression with {...}" in {
        val template = parseTemplateString(
          """@if(condition)    {good}     else        {somevar}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple("else"))
        ifExpression(3).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.content(0) mustBe Plain("somevar")
        ifExpression(3).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression.size must be(4)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if'/'else' expression with @{...}" in {
        val template = parseTemplateString(
          """@if(condition)    @{good}     else        @{somevar}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts(0) must be(
          Simple("{good}")
        )
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts.size must be(1)
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple("else"))
        ifExpression(3).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts(0) must be(
          Simple("{somevar}")
        )
        ifExpression(3).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts.size must be(1)
        ifExpression(3).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression.size must be(4)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if'/'else' expression with (...)" in {
        val template = parseTemplateString(
          """@if(condition)    (good)    else        (somevar)"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Simple] must be(Simple("(good)"))
        ifExpression(2).asInstanceOf[Simple] must be(Simple("else"))
        ifExpression(3).asInstanceOf[Simple] must be(Simple("(somevar)"))
        ifExpression.size must be(4)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if'/'else if' expression with {...}" in {
        val template = parseTemplateString(
          """@if(condition)    {good}     else if(true)        {somevar}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0) mustBe Plain("good")
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple("else if(true)"))
        ifExpression(3).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.content(0) mustBe Plain("somevar")
        ifExpression(3).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression.size must be(5)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if'/'else if' expression with @{...}" in {
        val template = parseTemplateString(
          """@if(condition)    @{good}     else if(true)        @{somevar}"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts(0) must be(
          Simple("{good}")
        )
        ifExpression(1).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts.size must be(1)
        ifExpression(1).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression(2).asInstanceOf[Simple] must be(Simple("else if(true)"))
        ifExpression(3).asInstanceOf[Block].contents.imports mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.members mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.sub mustBe empty
        ifExpression(3).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts(0) must be(
          Simple("{somevar}")
        )
        ifExpression(3).asInstanceOf[Block].contents.content(0).asInstanceOf[ScalaExp].parts.size must be(1)
        ifExpression(3).asInstanceOf[Block].contents.content.size must be(1)
        ifExpression.size must be(5)
        template.content.size must be(1)
      }

      "do not render whitespaces which are part of an 'if'/'else if' expression with (...)" in {
        val template = parseTemplateString(
          """@if(condition)    (good)    else if(true)        (somevar)"""
        )
        val ifExpression = template.content(0).asInstanceOf[Display].exp.parts
        ifExpression(0) must be(Simple("if(condition)"))
        ifExpression(1).asInstanceOf[Simple] must be(Simple("(good)"))
        ifExpression(2).asInstanceOf[Simple] must be(Simple("else if(true)"))
        ifExpression(3).asInstanceOf[Simple] must be(Simple("(somevar)"))
        ifExpression.size must be(5)
        template.content.size must be(1)
      }

      "parsing @x @{x}" in {
        val template = parseTemplateString("""@x @{x}""")
        template.content(0).asInstanceOf[Display].exp.parts.head must be(Simple("x"))
        template.content(0).asInstanceOf[Display].exp.parts.size must be(1)
        template.content(1).asInstanceOf[Plain] must be(Plain(" "))
        template.content(2).asInstanceOf[Display].exp.parts.head must be(Simple("{x}"))
        template.content(2).asInstanceOf[Display].exp.parts.size must be(1)
        template.content.size must be(3)
      }

      "parsing @{x} @x" in {
        val template = parseTemplateString("""@{x} @x""")
        template.content(0).asInstanceOf[Display].exp.parts.head must be(Simple("{x}"))
        template.content(0).asInstanceOf[Display].exp.parts.size must be(1)
        template.content(1).asInstanceOf[Plain] must be(Plain(" "))
        template.content(2).asInstanceOf[Display].exp.parts.head must be(Simple("x"))
        template.content(2).asInstanceOf[Display].exp.parts.size must be(1)
        template.content.size must be(3)
      }

      "parsing @{x} @{x}" in {
        val template = parseTemplateString("""@{x} @{x}""")
        template.content(0).asInstanceOf[Display].exp.parts.head must be(Simple("{x}"))
        template.content(0).asInstanceOf[Display].exp.parts.size must be(1)
        template.content(1).asInstanceOf[Plain] must be(Plain(" "))
        template.content(2).asInstanceOf[Display].exp.parts.head must be(Simple("{x}"))
        template.content(2).asInstanceOf[Display].exp.parts.size must be(1)
        template.content.size must be(3)
      }

      "parsing @x @x" in {
        val template = parseTemplateString("""@x @x""")
        template.content(0).asInstanceOf[Display].exp.parts.head must be(Simple("x"))
        template.content(0).asInstanceOf[Display].exp.parts.size must be(1)
        template.content(1).asInstanceOf[Plain] must be(Plain(" "))
        template.content(2).asInstanceOf[Display].exp.parts.head must be(Simple("x"))
        template.content(2).asInstanceOf[Display].exp.parts.size must be(1)
        template.content.size must be(3)
      }
    }

    "handle local val for pure code blocks" when {
      "lazy not also defined as val should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy field = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected 'val' after 'lazy'")
        )
      }
      // Even though variables can not have type parameters, we still try to parse them correctly
      // When they are parsed correctly, then we fail (see test below)
      "val, resultType is not given without implicit prefixed, has empty type params should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field[] = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "val, resultType is not given without implicit prefixed, has empty type params (with space) should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field[ ] = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "lazy val, resultType is not given without implicit prefixed, has empty type params should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field[] = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "lazy val, resultType is not given without implicit prefixed, has empty type params (with space) should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field[ ] = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      // Now the type params are parsed correctly, but still we disallow them for variables
      "val can not have type parameters" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field[FooType] = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have type parameters."
          )
        )
      }
      "lazy val can not have type parameters" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field[FooType] = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have type parameters."
          )
        )
      }
      "val can not have arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field(foo: String, bar: Int) = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "lazy val can not have arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field(foo: String, bar: Int) = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "val can not have empty arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field() = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "lazy val can not have empty arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field() = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "resultType is not given without implicit prefixed, not lazy" in {
        val tmpl = parseTemplateString(
          """@val field = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 6

        localMember.resultType mustBe None

        localMember match {
          case Val(name, isLazy, resultType, code) => isLazy mustBe false
          case _                                   => fail("Should be a Val!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is not given without implicit prefixed, lazy" in {
        val tmpl = parseTemplateString(
          """@lazy val field = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 11

        localMember.resultType mustBe None

        localMember match {
          case Val(name, isLazy, resultType, code) => isLazy mustBe true
          case _                                   => fail("Should be a Val!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
    }

    "handle local val for template code blocks" when {
      "lazy not also defined as val should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy field = { foo }"""
        ) must have(
          Symbol("message")("Template failed to parse: Expected 'val' after 'lazy'")
        )
      }
      // Even though variables can not have type parameters, we still try to parse them correctly
      // When they are parsed correctly, then we fail (see test below)
      "val, resultType is not given without implicit prefixed, has empty type params should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field[] = { foo }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "val, resultType is not given without implicit prefixed, has empty type params (with space) should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field[ ] = { foo }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "lazy val, resultType is not given without implicit prefixed, has empty type params should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field[] = { foo }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "lazy val, resultType is not given without implicit prefixed, has empty type params (with space) should fail" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field[ ] = { foo }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      // Now the type params are parsed correctly, but still we disallow them for variables
      "val can not have type parameters" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field[FooType] = { foo }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have type parameters."
          )
        )
      }
      "lazy val can not have type parameters" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field[FooType] = { foo }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have type parameters."
          )
        )
      }
      "val can not have arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field(foo: String, bar: Int) = { foo }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "lazy val can not have arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field(foo: String, bar: Int) = { foo }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "val can not have empty arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@val field() = { foo }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "lazy val can not have empty arguments" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy val field() = { foo }"""
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "resultType is not given without implicit prefixed, not lazy" in {
        val tmpl = parseTemplateString(
          """@val field = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "field"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 6

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe ""

        localSub.declaration mustBe Right(false) // (not lazy) val

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "resultType is not given without implicit prefixed, lazy" in {
        val tmpl = parseTemplateString(
          """@lazy val field = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "field"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 11

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe ""

        localSub.declaration mustBe Right(true) // lazy val

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
    }

    "handle local var for pure code blocks" when {
      "allow reassigning variable" in {
        val tmpl = parseTemplateString(
          """@var field = @{ "bar1" }
            |@field = @{ "bar2" }
            |@field""".stripMargin
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 6

        localMember.resultType mustBe None

        localMember match {
          case Var(name, resultType, code) => resultType mustBe None
          case _                           => fail("Should be a Var!")
        }

        localMember.code.code mustBe """{ "bar1" }"""
        tmpl.members.size mustBe 1
        tmpl.content(0) mustBe Plain("""
                                       |""".stripMargin)
        tmpl.content(1) mustBe Reassignment(Right(Var(PosString("field"), None, Simple("""{ "bar2" }"""))))
        tmpl.content(2) mustBe Plain("""
                                       |""".stripMargin)
        tmpl.content(3) mustBe Display(ScalaExp(ListBuffer(Simple("field"))))
        tmpl.content.size mustBe 4
      }

      "allow reassigning variable in if/elseif/else" in {
        val tmpl = parseTemplateString(
          """@(condition: Integer, secondCondition: Integer)
            |@var field = @{ "bar1" }
            |@if(condition == 1) {
            |  @field = @{ "bar2" }
            |} else if(secondCondition == 1) {
            |  @field = @{ "bar3" }
            |} else {
            |  @field = @{ "bar4" }
            |}
            |@field""".stripMargin
        )
        tmpl.params.str mustBe "(condition: Integer, secondCondition: Integer)"

        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 2
        localMember.name.pos.column mustBe 6

        localMember.resultType mustBe None

        localMember match {
          case Var(name, resultType, code) => resultType mustBe None
          case _                           => fail("Should be a Var!")
        }

        localMember.code.code mustBe """{ "bar1" }"""
        tmpl.members.size mustBe 1
        tmpl.content(0) mustBe Plain("""
                                       |""".stripMargin)
        tmpl.content(1) mustBe Display(
          ScalaExp(
            ListBuffer(
              Simple("if(condition == 1)"),
              Block(
                " ",
                None,
                BlockTemplate(
                  ArrayBuffer(),
                  ArrayBuffer(),
                  ArrayBuffer(),
                  ArrayBuffer(
                    Plain("""
                            |  """.stripMargin),
                    Reassignment(Right(Var(PosString("field"), None, Simple("""{ "bar2" }""")))),
                    Plain("""
                            |""".stripMargin)
                  )
                )
              ),
              Simple("else if(secondCondition == 1)"),
              Block(
                " ",
                None,
                BlockTemplate(
                  ArrayBuffer(),
                  ArrayBuffer(),
                  ArrayBuffer(),
                  ArrayBuffer(
                    Plain("""
                            |  """.stripMargin),
                    Reassignment(Right(Var(PosString("field"), None, Simple("""{ "bar3" }""")))),
                    Plain("""
                            |""".stripMargin)
                  )
                )
              ),
              Simple("else"),
              Block(
                "",
                None,
                BlockTemplate(
                  ArrayBuffer(),
                  ArrayBuffer(),
                  ArrayBuffer(),
                  ArrayBuffer(
                    Plain("""
                            |  """.stripMargin),
                    Reassignment(Right(Var(PosString("field"), None, Simple("""{ "bar4" }""")))),
                    Plain("""
                            |""".stripMargin)
                  )
                )
              )
            )
          )
        )
        tmpl.content(2) mustBe Plain("""
                                       |""".stripMargin)
        tmpl.content(3) mustBe Display(ScalaExp(ListBuffer(Simple("field"))))
        tmpl.content.size mustBe 4
      }

      "fail if type annotation present on variable reassignment" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field = @{ "bar1" }
            |@field:String = @{ "bar2" }""".stripMargin
        ) must have(
          Symbol("message")("Template failed to parse: Type annotation is not allowed on variable reassignment.")
        )
      }
      "lazy not allowed for var" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@lazy var foo = @{ "bar1" }"""
        ) must have(
          Symbol("message")("Template failed to parse: 'lazy' not allowed here. Only val definitions can be lazy.")
        )
      }
      "fail to reassign a function to a var" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field = @{ "bar1" }
            |@field() = @{ "bar2" }""".stripMargin
        ) must have(
          Symbol("message")(
            "Template failed to parse: field is already defined. To reassign field, remove any argument lists or type parameters. Otherwise choose a different name."
          )
        )
      }
      "fail to reassign a function with typ params and parens to a var" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field = @{ "bar1" }
            |@field[A]() = @{ "bar2" }""".stripMargin
        ) must have(
          Symbol("message")(
            "Template failed to parse: field is already defined. To reassign field, remove any argument lists or type parameters. Otherwise choose a different name."
          )
        )
      }
      "fail to reassign a function with typ params to a var" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field = @{ "bar1" }
            |@field[A] = @{ "bar2" }""".stripMargin
        ) must have(
          Symbol("message")(
            "Template failed to parse: field is already defined. To reassign field, remove any argument lists or type parameters. Otherwise choose a different name."
          )
        )
      }
      "fail to reassign when empty type params are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field = @{ "bar1" }
            |@field[] = @{ "bar2" }""".stripMargin
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "fail to reassign when empty type params that contain spaces are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field = @{ "bar1" }
            |@field[ ] = @{ "bar2" }""".stripMargin
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "fail to define var when empty type params are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field[] = @{ "bar1" }""".stripMargin
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "fail to define var when valid type params are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field[FooType] = @{ "bar1" }""".stripMargin
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have type parameters."
          )
        )
      }
      "fail to define var when valid type params and parens are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field[FooType]() = @{ "bar1" }""".stripMargin
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have type parameters."
          )
        )
      }
      "fail to define var when parameter list is given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field(foo: String, bar: Int) = @{ "bar1" }""".stripMargin
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
      "fail to define var when empty parameter list is given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@var field() = @{ "bar1" }""".stripMargin
        ) must have(
          Symbol("message")(
            "Template failed to parse: Invalid variable definition: 'field' cannot have parameter lists."
          )
        )
      }
    }

    "handle local pure code block definitions" when {
      "resultType is not given without implicit prefixed, no argument list" in {
        val tmpl = parseTemplateString(
          """@field = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is not given without implicit prefixed, empty argument list" in {
        val tmpl = parseTemplateString(
          """@field() = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe "()"
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is not given without implicit prefixed but prefixed with val" in {
        val tmpl = parseTemplateString(
          """@valfield = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "valfield"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is not given without implicit prefixed but prefixed with lazy" in {
        val tmpl = parseTemplateString(
          """@lazyfield = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "lazyfield"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is not given without implicit prefixed but prefixed with lazyval" in {
        val tmpl = parseTemplateString(
          """@lazyvalfield = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "lazyvalfield"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is not given without implicit prefixed, has type params" in {
        val tmpl = parseTemplateString(
          """@field[A,B](a:A, b:B) = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe "[A,B](a:A, b:B)"
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is given" in {
        val tmpl = parseTemplateString(
          """@implicitField: FieldConstructor = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "implicitField"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        val resultType = localMember.resultType.get
        resultType.str mustBe "FieldConstructor"
        resultType.pos.line mustBe 1
        resultType.pos.column mustBe 17

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is given without implicit prefixed" in {
        val tmpl = parseTemplateString(
          """@field: FieldConstructor = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "field"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        val resultType = localMember.resultType.get
        resultType.str mustBe "FieldConstructor"
        resultType.pos.line mustBe 1
        resultType.pos.column mustBe 9

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType with type is given" in {
        val tmpl = parseTemplateString(
          """@implicitField: FieldConstructor[FooType] = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "implicitField"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        val resultType = localMember.resultType.get
        resultType.str mustBe "FieldConstructor[FooType]"
        resultType.pos.line mustBe 1
        resultType.pos.column mustBe 17

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType is given without spaces" in {
        val tmpl = parseTemplateString(
          """@implicitField:FieldConstructor=@{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "implicitField"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        val resultType = localMember.resultType.get
        resultType.str mustBe "FieldConstructor"
        resultType.pos.line mustBe 1
        resultType.pos.column mustBe 16

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "resultType and params are given" in {
        val tmpl = parseTemplateString(
          """@implicitField(foo: String, bar: Int): FieldConstructor = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "implicitField"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        val resultType = localMember.resultType.get
        resultType.str mustBe "FieldConstructor"
        resultType.pos.line mustBe 1
        resultType.pos.column mustBe 40

        localMember match {
          case Def(name, params, resultType, code) =>
            params.str mustBe "(foo: String, bar: Int)"
            params.pos.line mustBe 1
            params.pos.column mustBe 15
          case _ => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "no resultType and no params are given" in {
        val tmpl = parseTemplateString(
          """@implicitField = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "implicitField"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) => params.str mustBe ""
          case _                                   => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "no resultType but params are given" in {
        val tmpl = parseTemplateString(
          """@implicitField(foo: String, bar: Int) = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        )
        val localMember = tmpl.members(0)

        localMember.name.str mustBe "implicitField"
        localMember.name.pos.line mustBe 1
        localMember.name.pos.column mustBe 2

        localMember.resultType mustBe None

        localMember match {
          case Def(name, params, resultType, code) =>
            params.str mustBe "(foo: String, bar: Int)"
            params.pos.line mustBe 1
            params.pos.column mustBe 15
          case _ => fail("Should be a Def!")
        }

        localMember.code.code mustBe "{ FieldConstructor(myFieldConstructorTemplate.f) }"
      }
      "empty type params are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@field[]() = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "empty type params that contain spaces are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@field[ ]() = @{ FieldConstructor(myFieldConstructorTemplate.f) }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
    }

    "handle local template code block definitions" when {
      "resultType is not given without implicit prefixed, no argument list" in {
        val tmpl = parseTemplateString(
          """@field = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "field"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe ""

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "resultType is not given without implicit prefixed, empty argument list" in {
        val tmpl = parseTemplateString(
          """@field() = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "field"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe "()"

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "resultType is not given without implicit prefixed but prefixed with val" in {
        val tmpl = parseTemplateString(
          """@valfield = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "valfield"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe ""

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "resultType is not given without implicit prefixed but prefixed with lazy" in {
        val tmpl = parseTemplateString(
          """@lazyfield = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "lazyfield"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe ""

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "resultType is not given without implicit prefixed but prefixed with lazyval" in {
        val tmpl = parseTemplateString(
          """@lazyvalfield = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "lazyvalfield"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe ""

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "resultType is not given without implicit prefixed, has type params" in {
        val tmpl = parseTemplateString(
          """@field[A,B](a:A, b:B) = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "field"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe "[A,B](a:A, b:B)"

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "not parse when resultType is given" in {
        val tmpl = parseTemplateString(
          """@implicitField: play.twirl.api.HtmlFormat.Appendable = { foo }"""
        )
        tmpl.sub mustBe empty
        tmpl.content mustBe Array(
          Display(ScalaExp(List(Simple("implicitField")))),
          Plain(": play.twirl.api.HtmlFormat.Appendable = "),
          Plain("{"),
          Plain(" "),
          Plain("foo "),
          Plain("}")
        )
      }
      "not parse when resultType is given without implicit prefixed" in {
        val tmpl = parseTemplateString(
          """@field: play.twirl.api.HtmlFormat.Appendable = { foo }"""
        )
        tmpl.sub mustBe empty
        tmpl.content mustBe Array(
          Display(ScalaExp(List(Simple("field")))),
          Plain(": play.twirl.api.HtmlFormat.Appendable = "),
          Plain("{"),
          Plain(" "),
          Plain("foo "),
          Plain("}")
        )
      }
      "not parse when resultType is given without spaces" in {
        val tmpl = parseTemplateString(
          """@implicitField:play.twirl.api.HtmlFormat.Appendable={ foo }"""
        )
        tmpl.sub mustBe empty
        tmpl.content mustBe Array(
          Display(ScalaExp(List(Simple("implicitField")))),
          Plain(":play.twirl.api.HtmlFormat.Appendable="),
          Plain("{"),
          Plain(" "),
          Plain("foo "),
          Plain("}")
        )
      }
      "not parse when resultType and params are given" in {
        val tmpl = parseTemplateString(
          """@implicitField(foo: String, bar: Int): FieldConstructor = { foo }"""
        )
        tmpl.sub mustBe empty
        tmpl.content mustBe Array(
          Display(ScalaExp(List(Simple("implicitField(foo: String, bar: Int)")))),
          Plain(": FieldConstructor = "),
          Plain("{"),
          Plain(" "),
          Plain("foo "),
          Plain("}")
        )
      }
      "no resultType and no params are given" in {
        val tmpl = parseTemplateString(
          """@implicitField = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "implicitField"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe ""

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "no resultType but params are given" in {
        val tmpl = parseTemplateString(
          """@implicitField(foo: String, bar: Int) = { foo }"""
        )
        val localSub = tmpl.sub(0)

        localSub.name.str mustBe "implicitField"
        localSub.name.pos.line mustBe 1
        localSub.name.pos.column mustBe 2

        localSub.members mustBe empty
        localSub.sub mustBe empty
        localSub.imports mustBe empty
        localSub.params.str mustBe "(foo: String, bar: Int)"
        localSub.params.pos.line mustBe 1
        localSub.params.pos.column mustBe 15

        localSub.declaration mustBe Left(false) // a def

        localSub.content mustBe Array(Plain(" "), Plain("foo "))
      }
      "empty type params are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@field[]() = { foo }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
      }
      "empty type params that contain spaces are given" in {
        the[RuntimeException] thrownBy parseTemplateString(
          """@field[ ]() = { foo }"""
        ) must have(
          Symbol("message")("Template failed to parse: identifier expected but ']' found")
        )
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
        parseFailure("unclosedBracket.scala.html", "Expected '}' but found 'EOF'", 16, 6)
      }

      "unclosedBracket2.scala.html" in {
        parseFailure("unclosedBracket2.scala.html", "Expected '}' but found 'EOF'", 36, 1)
      }

      "invalidAt.scala.html" in {
        parseFailure("invalidAt.scala.html", "Invalid '@' symbol", 9, 5)
      }
    }
  }
}
