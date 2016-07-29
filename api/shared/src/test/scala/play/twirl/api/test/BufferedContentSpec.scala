package play.twirl.api.test

import org.specs2.mutable._
import play.twirl.api._

import scala.collection.immutable

class BufferedContentSpec extends Specification {

  "equality checking" should {

    "return false for BufferedContents with the same body but different implementations" in {
      Html("hello") mustNotEqual Xml("hello")
      Html("hello") mustNotEqual Txt("hello")
      Xml("hello") mustNotEqual Txt("hello")
    }

    "return false for BufferedContents with different bodies but the same implementations" in {
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) mustNotEqual HtmlFormat.fill(immutable.Seq(Html("fizz"), Html("buzz")))
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) mustNotEqual Html("fizzbuzz")
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) mustNotEqual XmlFormat.fill(immutable.Seq(Xml("fizz"), Xml("buzz")))
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) mustNotEqual Xml("fizzbuzz")
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) mustNotEqual TxtFormat.fill(immutable.Seq(Txt("fizz"), Txt("buzz")))
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) mustNotEqual Txt("fizzbuzz")
      Html("hello") mustNotEqual Html("boom")
      Txt("hello") mustNotEqual Txt("boom")
      Xml("hello") mustNotEqual Xml("boom")
    }

    "return true for BufferedContents with the same body and the same implementation" in {
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) mustEqual HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar")))
      HtmlFormat.fill(immutable.Seq(Html("foo"), Html("bar"))) mustEqual Html("foobar")
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) mustEqual XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar")))
      XmlFormat.fill(immutable.Seq(Xml("foo"), Xml("bar"))) mustEqual Xml("foobar")
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) mustEqual TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar")))
      TxtFormat.fill(immutable.Seq(Txt("foo"), Txt("bar"))) mustEqual Txt("foobar")
      Html("hello") mustEqual Html("hello")
      Txt("hello") mustEqual Txt("hello")
      Xml("hello") mustEqual Xml("hello")
    }

  }

}
