/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.api.utils

object StringEscapeUtils {

  def escapeEcmaScript(input: String): String = {
    val s = new StringBuilder()
    val len = input.length
    var pos = 0
    while (pos < len) {
      input.charAt(pos) match {
        // Standard Lookup
        case '\'' => s.append("\\'")
        case '\"' => s.append("\\\"")
        case '\\' => s.append("\\\\")
        case '/' => s.append("\\/")
        // JAVA_CTRL_CHARS
        case '\b' => s.append("\\b")
        case '\n' => s.append("\\n")
        case '\t' => s.append("\\t")
        case '\f' => s.append("\\f")
        case '\r' => s.append("\\r")
        // Ignore any character below ' '
        case c if c < ' ' =>
        // if it not matches any characters above, just append it
        case c => s.append(c)
      }
      pos += 1
    }

    s.toString()
  }

  def escapeXml11(input: String): String = throw new RuntimeException("XML is not Supported on ScalaJS")

}
