package play.twirl.api
package test

import org.specs2.mutable._

class StringInterpolationSpec extends Specification {

  "StringInterpolation" should {
    "leave string parts untouched" in {
      val p = html"<p>"
      p.body must_== "<p>"
    }
    "escape interpolated arguments" in {
      val arg = "<"
      val p = html"<p>$arg</p>"
      p.body must_== "<p>&lt;</p>"
    }
    "leave nested templates untouched" in {
      val p = html"<p></p>"
      val div = html"<div>$p</div>"
      div.body must_== "<div><p></p></div>"
    }
    "display arguments as they would be displayed in a template" in {
      html"${Some("a")} $None".body must_== "a "
      html"${Seq("a", "b")}".body must_== "ab"
    }
  }

}