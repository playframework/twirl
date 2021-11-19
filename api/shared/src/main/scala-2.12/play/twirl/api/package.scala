/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl

import scala.reflect.ClassTag

package object api {

  /**
   * Brings the template engine as a
   * [[https://docs.scala-lang.org/overviews/core/string-interpolation.html string interpolator]].
   *
   * Basic usage:
   *
   * {{{
   *   import play.twirl.api.StringInterpolation
   *
   *   val name = "Martin"
   *   val htmlFragment: Html = html"&lt;div&gt;Hello \$name&lt;/div&gt;"
   * }}}
   *
   * Three interpolators are available: `html`, `xml` and `js`.
   */
  implicit class StringInterpolation(val sc: StringContext) extends AnyVal {
    def html(args: Any*): Html = interpolate(args, HtmlFormat)

    def xml(args: Any*): Xml = interpolate(args, XmlFormat)

    def js(args: Any*): JavaScript = interpolate(args, JavaScriptFormat)

    def interpolate[A <: Appendable[A]: ClassTag](args: Seq[Any], format: Format[A]): A = {
      sc.checkLengths(args)
      val array       = Array.ofDim[Any](args.size + sc.parts.size)
      val strings     = sc.parts.iterator
      val expressions = args.iterator
      array(0) = format.raw(strings.next())
      var i = 1
      while (strings.hasNext) {
        array(i) = expressions.next()
        array(i + 1) = format.raw(strings.next())
        i += 2
      }
      new BaseScalaTemplate[A, Format[A]](format)._display_(array)
    }
  }
}
