/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.compiler
package test

import org.specs2.mutable._
import twirl.api._

object TemplateUtilsSpec extends Specification {

  "Templates" should {

    "provide a HASH util" in {
      Hash("itShouldWork".getBytes, "") must be_==("31c0c4e0e142fe9b605fff44528fedb3dd8ae254")
    }

    "provide a Format API" in {

      "HTML for example" in {

        case class Html(_text: String) extends BufferedContent[Html](Nil, _text) {
          val contentType = "text/html"
        }

        object HtmlFormat extends Format[Html] {
          def raw(text: String) = Html(text)
          def escape(text: String) = Html(text.replace("<", "&lt;"))
          def empty = Html("")
          def fill(elements: TraversableOnce[Html]) = Html("")
        }

        val html = HtmlFormat.raw("<h1>").body + HtmlFormat.escape("Hello <world>").body + HtmlFormat.raw("</h1>").body

        html must be_==("<h1>Hello &lt;world></h1>")
      }

      "Text for example" in {

        case class Text(_text: String) extends BufferedContent[Text](Nil, _text) {
          val contentType = "text/plain"
        }

        object TextFormat extends Format[Text] {
          def raw(text: String) = Text(text)
          def escape(text: String) = Text(text)
          def empty = Text("")
          def fill(elements: TraversableOnce[Text]) = Text("")
        }

        val text = TextFormat.raw("<h1>").body + TextFormat.escape("Hello <world>").body + TextFormat.raw("</h1>").body

        text must be_==("<h1>Hello <world></h1>")

      }
    }
  }
}
