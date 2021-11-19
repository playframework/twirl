/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.api

import java.util.Optional
import scala.collection.immutable
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

case class BaseScalaTemplate[T <: Appendable[T], F <: Format[T]](format: F) {
  // The overloaded methods are here for speed. The compiled templates
  // can take advantage of them for a 12% performance boost
  def _display_(x: AnyVal): T            = format.escape(x.toString)
  def _display_(x: String): T            = if (x eq null) format.empty else format.escape(x)
  def _display_(x: Unit): T              = format.empty
  def _display_(x: scala.xml.NodeSeq): T = if (x eq null) format.empty else format.raw(x.toString())
  def _display_(x: T): T                 = if (x eq null) format.empty else x

  def _display_(o: Any)(implicit m: ClassTag[T]): T = {
    o match {
      case escaped if escaped != null && escaped.getClass == m.runtimeClass => escaped.asInstanceOf[T]
      case ()                                                               => format.empty
      case None                                                             => format.empty
      case Some(v)                                                          => _display_(v)
      case key: Optional[_] =>
        (if (key.isPresent) Some(key.get) else None) match {
          case None    => format.empty
          case Some(v) => _display_(v)
          case null    => format.empty
        }
      case xml: scala.xml.NodeSeq       => format.raw(xml.toString())
      case escapeds: immutable.Seq[_]   => format.fill(escapeds.map(_display_))
      case escapeds: TraversableOnce[_] => format.fill(escapeds.iterator.map(_display_).toList)
      case escapeds: Array[_]           => format.fill(escapeds.view.map(_display_).toList)
      case escapeds: java.util.List[_] =>
        format.fill(escapeds.asScala.map(_display_).toList)
      case string: String => format.escape(string)
      case v if v != null => format.escape(v.toString)
      case null           => format.empty
    }
  }
}
