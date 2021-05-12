/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.api

import scala.language.implicitConversions

@deprecated("Use TwirlDefaultImports instead", "1.2.0")
object TemplateMagic {
  // --- UTILS

  def defining[T](t: T)(handler: T => Any) = {
    handler(t)
  }

  def using[T](t: T) = t

  // --- IF

  implicit def iterableToBoolean(x: Iterable[_]): Boolean = x != null && !x.isEmpty
  implicit def optionToBoolean(x: Option[_]): Boolean     = x != null && x.isDefined
  implicit def stringToBoolean(x: String): Boolean        = x != null && !x.isEmpty

  // --- JAVA

  implicit def javaCollectionToScala[T](x: java.lang.Iterable[T]): Iterable[T] = {
    import scala.collection.JavaConverters._
    x.asScala
  }

  // --- DEFAULT

  case class Default(default: Any) {
    def ?:(x: Any) =
      x match {
        case ""    => default
        case Nil   => default
        case false => default
        case 0     => default
        case None  => default
        case _     => x
      }
  }

  implicit def anyToDefault(x: Any): Default = Default(x)

  // --- DATE

  class RichDate(date: java.util.Date) {
    def format(pattern: String) = {
      new java.text.SimpleDateFormat(pattern).format(date)
    }
  }

  implicit def richDate(date: java.util.Date): RichDate = new RichDate(date)

  // --- STRING

  class RichString(string: String) {
    def when(predicate: => Boolean) = {
      predicate match {
        case true  => string
        case false => ""
      }
    }
  }

  implicit def richString(string: String): RichString = new RichString(string)
}
