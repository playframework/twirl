/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.api.test

import play.twirl.api._
import scala.collection.immutable
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BufferedContentSpec extends AnyWordSpec with Matchers {

  "equality checking" should {
    "return false for BufferedContents with the same body but different implementations" in {
      Html("hello") must not be Xml("hello")
      Html("hello") must not be Txt("hello")
      Xml("hello") must not be Txt("hello")
    }

    "return false for BufferedContents with different bodies but the same implementations" in {
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) must not be HtmlFormat.fill(
        immutable.Seq(Html("fizz"), Html("buzz"))
      )
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) must not be Html("fizzbuzz")
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) must not be XmlFormat.fill(
        immutable.Seq(Xml("fizz"), Xml("buzz"))
      )
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) must not be Xml("fizzbuzz")
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) must not be TxtFormat.fill(
        immutable.Seq(Txt("fizz"), Txt("buzz"))
      )
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) must not be Txt("fizzbuzz")
      Html("hello") must not be Html("boom")
      Txt("hello") must not be Txt("boom")
      Xml("hello") must not be Xml("boom")
    }

    "return true for BufferedContents with the same body and the same implementation" in {
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) mustEqual HtmlFormat.fill(
        immutable.Seq(Html("foo"), Html("bar"))
      )
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) mustEqual Html("foobar")
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) mustEqual XmlFormat.fill(
        immutable.Seq(Xml("foo"), Xml("bar"))
      )
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) mustEqual Xml("foobar")
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) mustEqual TxtFormat.fill(
        immutable.Seq(Txt("foo"), Txt("bar"))
      )
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) mustEqual Txt("foobar")
      Html("hello") mustEqual Html("hello")
      Txt("hello") mustEqual Txt("hello")
      Xml("hello") mustEqual Xml("hello")
    }
  }
}
