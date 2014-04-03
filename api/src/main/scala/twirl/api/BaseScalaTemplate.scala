/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.api

case class BaseScalaTemplate[T <: Appendable[T], F <: Format[T]](format: F) {

  // The overloaded methods are here for speed. The compiled templates
  // can take advantage of them for a 12% performance boost
  def _display_(x: AnyVal): T = format.escape(x.toString)
  def _display_(x: String): T = format.escape(x)
  def _display_(x: Unit): T = format.empty
  def _display_(x: scala.xml.NodeSeq): T = format.raw(x.toString)
  def _display_(x: T): T = x

  def _display_(o: Any)(implicit m: Manifest[T]): T = {
    o match {
      case escaped if escaped != null && escaped.getClass == m.erasure => escaped.asInstanceOf[T]
      case () => format.empty
      case None => format.empty
      case Some(v) => _display_(v)
      case xml: scala.xml.NodeSeq => format.raw(xml.toString)
      case escapeds: TraversableOnce[_] => format.fill(escapeds.map(_display_(_)))
      case escapeds: Array[_] => format.fill(escapeds.toIterator.map(_display_(_)))
      case string: String => format.escape(string)
      case v if v != null => format.escape(v.toString)
      case _ => format.empty
    }
  }

}
