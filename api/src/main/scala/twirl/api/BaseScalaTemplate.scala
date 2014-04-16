/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.api

import scala.collection.immutable

case class BaseScalaTemplate[T <: Appendable[T], F <: Format[T]](format: F) {

  // The overloaded methods are here for speed. The compiled templates
  // can take advantage of them for a 12% performance boost
  def _display_(x: AnyVal): T = format.escape(x.toString)
  def _display_(x: String): T = format.escape(x)
  def _display_(x: Unit): T = format.empty
  def _display_(x: scala.xml.NodeSeq): T = format.raw(x.toString())
  def _display_(x: T): T = x

  def _display_(o: Any)(implicit m: Manifest[T]): T = {
    import twirl.api.ScalaCompat._ // for Manifest.runtimeClass
    o match {
      case escaped if escaped != null && escaped.getClass == m.runtimeClass => escaped.asInstanceOf[T]
      case () => format.empty
      case None => format.empty
      case Some(v) => _display_(v)
      case xml: scala.xml.NodeSeq => format.raw(xml.toString())
      case escapeds: immutable.Seq[_] => format.fill(escapeds.map(_display_))
      case escapeds: TraversableOnce[_] => format.fill(immutableSeq(escapeds.map(_display_)))
      case escapeds: Array[_] => format.fill(immutableSeq(escapeds.view.map(_display_)))
      case string: String => format.escape(string)
      case v if v != null => format.escape(v.toString)
      case _ => format.empty
    }
  }

  /**
   * For scala 2.9 compatibility, which doesn't have `to[immutable.Seq]`.
   */
  private def immutableSeq[A](seq: TraversableOnce[A]): immutable.Seq[A] = {
    val b = Vector.newBuilder[A]
    b ++= seq
    b.result
  }
}
