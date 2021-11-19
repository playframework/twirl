/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.api

import scala.language.implicitConversions

/**
 * Imports for useful Twirl helpers.
 */
object TwirlHelperImports {

  /** Allows Java collections to be used as if they were Scala collections. */
  implicit def twirlJavaCollectionToScala[T](x: java.lang.Iterable[T]): Iterable[T] = {
    import scala.collection.JavaConverters._
    x.asScala
  }

  /** Allows inline formatting of java.util.Date */
  implicit class TwirlRichDate(date: java.util.Date) {
    def format(pattern: String): String = {
      new java.text.SimpleDateFormat(pattern).format(date)
    }
  }

  /** Adds a when method to Strings to control when they are rendered. */
  implicit class TwirlRichString(string: String) {
    def when(predicate: => Boolean): String = {
      predicate match {
        case true  => string
        case false => ""
      }
    }
  }
}
