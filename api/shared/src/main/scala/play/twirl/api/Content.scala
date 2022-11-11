/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.api

import scala.collection.immutable

/**
 * Generic type representing content to be sent over an HTTP response.
 */
trait Content {

  /**
   * The content String.
   */
  def body: String

  /**
   * The default Content type to use for this content.
   */
  def contentType: String
}

/**
 * Appendable content using a StringBuilder. Either specify elements or text, not both.
 *
 * Using an Either[TraversableOnce[A], String] impacts performance in an already contentious part of code, so it has
 * been done with both parameters instead.
 *
 * @param elements
 *   Sub elements to traverse when creating the resultant string
 * @param text
 *   Formatted content
 * @tparam A
 *   self-type
 */
abstract class BufferedContent[A <: BufferedContent[A]](
    protected val elements: immutable.Seq[A],
    protected val text: String
) extends Appendable[A]
    with Content { this: A =>
  protected def buildString(builder: StringBuilder): Unit = {
    if (!elements.isEmpty) {
      elements.foreach { e => e.buildString(builder) }
    } else {
      builder.append(text)
    }
  }

  /**
   * This should only ever be called at the top level element to avoid unneeded memory allocation.
   */
  private lazy val builtBody = {
    val builder = new StringBuilder()
    buildString(builder)
    builder.toString
  }

  override def toString = builtBody

  def body = builtBody

  override def equals(obj: Any): Boolean =
    obj match {
      case other: BufferedContent[_] if this.getClass == other.getClass => body == other.body
      case _                                                            => false
    }

  override def hashCode(): Int = this.getClass.hashCode() + body.hashCode()
}
