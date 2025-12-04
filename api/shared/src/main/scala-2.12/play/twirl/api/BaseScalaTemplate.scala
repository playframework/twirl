/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.api

import java.util.Optional
import scala.collection.immutable
import scala.collection.JavaConverters
import scala.reflect.ClassTag

// The exotic name $twirl__format is on purpose to avoid clashes with user defined vars in templates (#112)
case class BaseScalaTemplate[T <: Appendable[T], F <: Format[T]]($twirl__format: F) {
  // The overloaded methods are here for speed. The compiled templates
  // can take advantage of them for a 12% performance boost
  def _display_(x: AnyVal): T            = $twirl__format.escape(x.toString)
  def _display_(x: String): T            = if (x eq null) $twirl__format.empty else $twirl__format.escape(x)
  def _display_(x: Unit): T              = $twirl__format.empty
  def _display_(x: scala.xml.NodeSeq): T = if (x eq null) $twirl__format.empty else $twirl__format.raw(x.toString())
  def _display_(x: T): T                 = if (x eq null) $twirl__format.empty else x

  def _display_(o: Any)(implicit m: ClassTag[T]): T = {
    o match {
      case escaped if escaped != null && escaped.getClass == m.runtimeClass => escaped.asInstanceOf[T]
      case ()                                                               => $twirl__format.empty
      case None                                                             => $twirl__format.empty
      case Some(v)                                                          => _display_(v)
      case key: Optional[?]                                                 =>
        (if (key.isPresent) Some(key.get) else None) match {
          case None    => $twirl__format.empty
          case Some(v) => _display_(v)
          case _       => $twirl__format.empty
        }
      case xml: scala.xml.NodeSeq       => $twirl__format.raw(xml.toString())
      case escapeds: immutable.Seq[?]   => $twirl__format.fill(escapeds.map(_display_))
      case escapeds: TraversableOnce[?] => $twirl__format.fill(escapeds.map(_display_).toList)
      case escapeds: Array[?]           => $twirl__format.fill(escapeds.view.map(_display_).toList)
      case escapeds: java.util.List[?]  =>
        $twirl__format.fill(JavaConverters.collectionAsScalaIterableConverter(escapeds).asScala.map(_display_).toList)
      case string: String => $twirl__format.escape(string)
      case v if v != null => $twirl__format.escape(v.toString)
      case _              => $twirl__format.empty
    }
  }
}
