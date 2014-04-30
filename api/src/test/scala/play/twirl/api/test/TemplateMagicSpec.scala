package play.twirl.api
package test

import org.specs2.mutable._

object TemplateMagicSpec extends Specification {
  import TemplateMagic._

  "TemplateMagic" should {
    "implicitly convert Option[_] to Boolean" in {
      val value: Boolean = Some("foo")
      value must beTrue
    }

    "implicit convert Some(true) to true" in {
      val value: Boolean = Some(true)
      value must beTrue
    }

    "implicit convert Some(false) to false" in {
      val value: Boolean = Some(false)
      value must beFalse
    }
  }

}