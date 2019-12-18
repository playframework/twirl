/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.api
package test

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FormatSpec extends AnyWordSpec with Matchers {

  "Formats" should {
    "show null text values as empty" in {
      val text: String = null

      Html(text).body mustBe empty
      new Html(text).body mustBe empty

      Txt(text).body mustBe empty
      new Txt(text).body mustBe empty

      Xml(text).body mustBe empty
      new Xml(text).body mustBe empty

      JavaScript(text).body mustBe empty
      new JavaScript(text).body mustBe empty
    }
  }

  "HtmlFormat" should {
    "escape '<', '&' and '>'" in {
      HtmlFormat.escape("foo < bar & baz >").body mustBe "foo &lt; bar &amp; baz &gt;"
    }

    "escape single quotes" in {
      HtmlFormat.escape("'single quotes'").body mustBe "&#x27;single quotes&#x27;"
    }

    "escape double quotes" in {
      HtmlFormat.escape("\"double quotes\"").body mustBe "&quot;double quotes&quot;"
    }

    "not escape non-ASCII characters" in {
      HtmlFormat.escape("こんにちは").body mustBe "こんにちは"
    }
  }

  "JavaScriptFormat" should {
    """escape ''', '"' and '\'""" in {
      JavaScriptFormat.escape("""foo ' bar " baz \""").body must be("""foo \' bar \" baz \\""")
    }
  }
}
