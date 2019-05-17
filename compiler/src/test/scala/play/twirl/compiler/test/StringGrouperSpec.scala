/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.compiler
package test

import java.io._

import org.scalatest.{ MustMatchers, WordSpec }

class StringGrouperSpec extends WordSpec with MustMatchers {
  val beer = "\uD83C\uDF7A"
  val line = "abcde" + beer + "fg"

  "StringGrouper" should {

    "split before a surrogate pair" in {
      StringGrouper(line, 5) must contain allOf ("abcde", beer + "fg")
    }

    "not split a surrogate pair" in {
      StringGrouper(line, 6) must contain allOf("abcde" + beer, "fg")
    }

    "split after a surrogate pair" in {
      StringGrouper(line, 7) must contain allOf("abcde" + beer, "fg")
    }
  }
}