/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.api

import scala.collection.immutable

/**
 * A type that works with BaseScalaTemplate
 * This used to support +=, but no longer is required to.
 * @todo Change name to reflect not appendable
 */
trait Appendable[T]

/**
 * A template format defines how to properly integrate content for a type `T` (e.g. to prevent cross-site scripting attacks)
 * @tparam T The underlying type that this format applies to.
 */
trait Format[T <: Appendable[T]] {
  type Appendable = T

  /**
   * Integrate `text` without performing any escaping process.
   * @param text Text to integrate
   */
  def raw(text: String): T

  /**
   * Integrate `text` after escaping special characters. e.g. for HTML, “<” becomes “&amp;lt;”
   * @param text Text to integrate
   */
  def escape(text: String): T

  /**
   * Generate an empty appendable
   */
  def empty: T

  /**
   * Fill an appendable with the elements
   */
  def fill(elements: immutable.Seq[T]): T
}
