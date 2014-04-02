package play.templates.test

import org.specs2.mutable._

import play.templates._

object TemplateParserSpec extends Specification {

  "The template parser" should {

    import scala.util.parsing.input.CharSequenceReader

    val parser = new ScalaTemplateParser(false)

    def get(templateName: String) = {
      val source = scala.io.Source.fromFile("parser/src/test/templates/" + templateName)
      val contents = source.mkString
      source.close()
      contents
    }

    def parse(templateName: String) = {
      (new ScalaTemplateParser(false)).parse(get(templateName))
    }

    def failAt(message: String, line: Int, column: Int): PartialFunction[parser.ParseResult, Boolean] = {
      case parser.Error(_, rest, msgs) => {
        message == msgs.head.toString && rest.pos.line == line && rest.pos.column == column
      }
    }

    "succeed for" in {

      "static.scala.html" in {
        parse("static.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "simple.scala.html" in {
        parse("simple.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "complicated.scala.html" in {
        parse("complicated.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

    }

    "fail for" in {

      "unclosedBracket.scala.html" in {
        parse("unclosedBracket.scala.html") must beLike({
          case parser.Error(_, rest, msgs) => {
            if (msgs.head.toString == "[ERROR] Expected '}' but found: 'EOF'.") ok else ko
          }
        })
      }

      "unclosedBracket2.scala.html" in {
        parse("unclosedBracket2.scala.html") must beLike({
          case parser.Error(_, rest, msgs) => {
            if (msgs.head.toString == "[ERROR] Expected '}' but found: 'EOF'.") ok else ko
          }
        })
      }

      "invalidAt.scala.html" in {
        parse("invalidAt.scala.html") must beLike({
          case parser.Error(_, rest, msgs) => {
            val (msg, pos) = (msgs.head.toString, msgs.head.pos)
            if (msg.contains("[ERROR] Invalid '@' symbol.") && pos.line == 5 && pos.column == 6) ok else ko
          }
        })
      }

    }

  }

}
