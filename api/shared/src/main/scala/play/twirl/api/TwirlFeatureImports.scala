/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.api

import scala.language.implicitConversions

/**
 * Imports that provide Twirl language features.
 *
 * This includes:
 *
 *   - \@defining
 *   - \@using
 *   - iterable/option/string as boolean for if statements
 *   - default values (maybeFoo ? defaultFoo)
 */
object TwirlFeatureImports {

  /**
   * Provides the `@defining` language feature, that lets you set a local val that can be reused.
   *
   * @param t
   *   The defined val.
   * @param handler
   *   The block to handle it.
   */
  def defining[T](t: T)(handler: T => Any): Any = {
    handler(t)
  }

  /** Provides the `@using` language feature. */
  def using[T](t: T): T = t

  /** Adds "truthiness" to iterables, making them false if they are empty. */
  implicit def twirlIterableToBoolean(x: Iterable[_]): Boolean = x != null && !x.isEmpty

  /** Adds "truthiness" to options, making them false if they are empty. */
  implicit def twirlOptionToBoolean(x: Option[_]): Boolean = x != null && x.isDefined

  /** Adds "truthiness" to strings, making them false if they are empty. */
  implicit def twirlStringToBoolean(x: String): Boolean = x != null && !x.isEmpty

  /**
   * Provides default values, such that an empty sequence, string, option, false boolean, or null will render the
   * default value.
   */
  implicit class TwirlDefaultValue(default: Any) {
    def ?:(x: Any): Any =
      x match {
        case ""    => default
        case Nil   => default
        case false => default
        case 0     => default
        case None  => default
        case _     => x
      }
  }
}
