/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.api

import scala.collection.immutable
import scala.reflect.ClassTag

case class BaseScalaTemplate[T <: Appendable[T], F <: Format[T]](format: F) {

  // The overloaded methods are here for speed. The compiled templates
  // can take advantage of them for a 12% performance boost
  def _display_(x: AnyVal): T = format.escape(x.toString)
  def _display_(x: String): T = if (x eq null) format.empty else format.escape(x)
  def _display_(x: Unit): T = format.empty
  def _display_(x: T): T = if (x eq null) format.empty else x

  def _display_(o: Any)(implicit m: ClassTag[T]): T = {
    o match {
      case escaped if escaped != null && escaped.getClass == m.runtimeClass => escaped.asInstanceOf[T]
      case () => format.empty
      case None => format.empty
      case Some(v) => _display_(v)
      case escapeds: immutable.Seq[_] => format.fill(escapeds.map(_display_))
      case escapeds: TraversableOnce[_] => format.fill(escapeds.map(_display_).to[immutable.Seq])
      case escapeds: Array[_] => format.fill(escapeds.view.map(_display_).to[immutable.Seq])
      case string: String => format.escape(string)
      case v if v != null => format.escape(v.toString)
      case _ => format.empty
    }
  }
}
