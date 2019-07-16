/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.api

import play.twirl.api.utils.StringEscapeUtils
import scala.collection.immutable
import java.lang.{StringBuilder => jStringBuilder}

object MimeTypes {
  val TEXT       = "text/plain"
  val HTML       = "text/html"
  val XML        = "application/xml"
  val JAVASCRIPT = "application/javascript"
}

object Formats {
  def safe(text: String): String = if (text eq null) "" else text
}

/**
 * Content type used in default HTML templates.
 *
 * This has 3 states, either it's a tree of elements, or a leaf, if it's a leaf, it's either safe text, or unsafe text
 * that needs to be escaped when written out.
 */
class Html private[api] (elements: immutable.Seq[Html], text: String, escape: Boolean) extends BufferedContent[Html](elements, text) {
  def this(text: String) = this(Nil, Formats.safe(text), false)
  def this(elements: immutable.Seq[Html]) = this(elements, "", false)

  /**
   * We override buildString for performance - allowing text to not be escaped until passed in the final StringBuilder
   * to encode it into.
   *
   * An alternative way of implementing this would be to make HtmlFormat.escape return a subclass of Html with a custom
   * buildString implementation.  While this does significantly improve performance if a template needs to escape a lot
   * of Strings, if it doesn't, performance actually goes down (measured 10%), due to the fact that the JVM can't
   * optimise the invocation of buildString as well because there are two different possible implementations.
   */
  override protected def buildString(sb: jStringBuilder): Unit = {
    if (!elements.isEmpty) {
      elements.foreach { e =>
        e.buildString(sb)
      }
    } else if (escape) {
      // Using our own algorithm here because commons lang escaping wasn't designed for protecting against XSS, and there
      // don't seem to be any other good generic escaping tools out there.
      val len = text.length
      var copyIdx = 0
      var i = 0
      while (i < len) {
        text.charAt(i) match {
          case '<' | '>' | '"' | '\'' | '&' => {
            sb.append(text, copyIdx, i)
            text.charAt(i) match {
              case '<' => sb.append("&lt;")
              case '>' => sb.append("&gt;")
              case '"' => sb.append("&quot;")
              case '\'' => sb.append("&#x27;")
              case '&' => sb.append("&amp;")
            }
            copyIdx = i + 1
          }
          case _ => ()
        }
        i += 1
      }
      if (copyIdx == 0) sb.append(text)
      else sb.append(text, copyIdx, len)
    } else {
      sb.append(text)
    }
  }

  /**
   * Content type of HTML.
   */
  val contentType = MimeTypes.HTML
}

/**
 * Helper for HTML utility methods.
 */
object Html {

  /**
   * Creates an HTML fragment with initial content specified.
   */
  def apply(text: String): Html = {
    new Html(text)
  }

  /**
    * Creates an HTML fragment with initial content specified. Uses an empty String if None is passed.
    */
  def apply(text: Option[String]): Html = {
    apply(text.getOrElse(""))
  }
}

/**
 * Formatter for HTML content.
 */
object HtmlFormat extends Format[Html] {

  /**
   * Creates a raw (unescaped) HTML fragment.
   */
  def raw(text: String): Html = Html(text)

  /**
   * Creates a safe (escaped) HTML fragment.
   */
  def escape(text: String): Html = {
    new Html(Nil, text, true)
  }

  /**
   * Generate an empty HTML fragment
   */
  val empty: Html = new Html("")

  /**
   * Create an HTML Fragment that holds other fragments.
   */
  def fill(elements: immutable.Seq[Html]): Html = new Html(elements)

}

/**
 * Content type used in default text templates.
 */
class Txt private (elements: immutable.Seq[Txt], text: String) extends BufferedContent[Txt](elements, text) {
  def this(text: String) = this(Nil, Formats.safe(text))
  def this(elements: immutable.Seq[Txt]) = this(elements, "")

  /**
   * Content type of text (`text/plain`).
   */
  def contentType = MimeTypes.TEXT
}

/**
 * Helper for utilities Txt methods.
 */
object Txt {

  /**
   * Creates a text fragment with initial content specified.
   */
  def apply(text: String): Txt = {
    new Txt(text)
  }

}

/**
 * Formatter for text content.
 */
object TxtFormat extends Format[Txt] {

  /**
   * Create a text fragment.
   */
  def raw(text: String) = Txt(text)

  /**
   * No need for a safe (escaped) text fragment.
   */
  def escape(text: String) = Txt(text)

  /**
   * Generate an empty Txt fragment
   */
  val empty: Txt = new Txt("")

  /**
   * Create an Txt Fragment that holds other fragments.
   */
  def fill(elements: immutable.Seq[Txt]): Txt = new Txt(elements)

}

/**
 * Content type used in default XML templates.
 */
class Xml private (elements: immutable.Seq[Xml], text: String) extends BufferedContent[Xml](elements, text) {
  def this(text: String) = this(Nil, Formats.safe(text))
  def this(elements: immutable.Seq[Xml]) = this(elements, "")

  /**
   * Content type of XML (`application/xml`).
   */
  def contentType = MimeTypes.XML
}

/**
 * Helper for XML utility methods.
 */
object Xml {

  /**
   * Creates an XML fragment with initial content specified.
   */
  def apply(text: String): Xml = {
    new Xml(text)
  }

}

/**
 * Formatter for XML content.
 */
object XmlFormat extends Format[Xml] {

  /**
   * Creates an XML fragment.
   */
  def raw(text: String) = Xml(text)

  /**
   * Creates an escaped XML fragment.
   */
  def escape(text: String) = Xml(StringEscapeUtils.escapeXml11(text))

  /**
   * Generate an empty XML fragment
   */
  val empty: Xml = new Xml("")

  /**
   * Create an XML Fragment that holds other fragments.
   */
  def fill(elements: immutable.Seq[Xml]): Xml = new Xml(elements)

}

/**
 * Type used in default JavaScript templates.
 */
class JavaScript private (elements: immutable.Seq[JavaScript], text: String) extends BufferedContent[JavaScript](elements, text) {
  def this(text: String) = this(Nil, Formats.safe(text))
  def this(elements: immutable.Seq[JavaScript]) = this(elements, "")

  /**
   * Content type of JavaScript
   */
  val contentType = MimeTypes.JAVASCRIPT
}

/**
 * Helper for JavaScript utility methods.
 */
object JavaScript {
  /**
   * Creates a JavaScript fragment with initial content specified
   */
  def apply(text: String) = {
    new JavaScript(text)
  }
}

/**
 * Formatter for JavaScript content.
 */
object JavaScriptFormat extends Format[JavaScript] {
  /**
   * Integrate `text` without performing any escaping process.
   * @param text Text to integrate
   */
  def raw(text: String): JavaScript = JavaScript(text)

  /**
   * Escapes `text` using JavaScript String rules.
   * @param text Text to integrate
   */
  def escape(text: String): JavaScript = JavaScript(StringEscapeUtils.escapeEcmaScript(text))

  /**
   * Generate an empty JavaScript fragment
   */
  val empty: JavaScript = new JavaScript("")

  /**
   * Create an JavaScript Fragment that holds other fragments.
   */
  def fill(elements: immutable.Seq[JavaScript]): JavaScript = new JavaScript(elements)

}
