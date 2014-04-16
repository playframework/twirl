/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
 package play.twirl.api
 package test

import org.specs2.mutable._

object FormatSpec extends Specification {
  "HtmlFormat" should {
    "escape '<', '&' and '>'" in {
      HtmlFormat.escape("foo < bar & baz >").body must_== "foo &lt; bar &amp; baz &gt;"
    }

    "escape single quotes" in {
      HtmlFormat.escape("'single quotes'").body must_== "&#x27;single quotes&#x27;"
    }

    "escape double quotes" in {
      HtmlFormat.escape("\"double quotes\"").body must_== "&quot;double quotes&quot;"
    }

    "not escape non-ASCII characters" in {
      HtmlFormat.escape("こんにちは").body must_== "こんにちは"
    }
  }

  "JavaScriptFormat" should {
    """escape ''', '"' and '\'""" in {
      JavaScriptFormat.escape("""foo ' bar " baz \""").body must equalTo ("""foo \' bar \" baz \\""")
    }
  }

}
