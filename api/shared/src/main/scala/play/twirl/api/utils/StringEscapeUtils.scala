/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.api.utils

object StringEscapeUtils {
  def escapeEcmaScript(input: String): String = {
    val s   = new StringBuilder()
    val len = input.length
    var pos = 0
    while (pos < len) {
      input.charAt(pos) match {
        // Standard Lookup
        case '\'' => s.append("\\'")
        case '\"' => s.append("\\\"")
        case '\\' => s.append("\\\\")
        case '/'  => s.append("\\/")
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

  def escapeXml11(input: String): String = {
    // Implemented per XML spec:
    // http://www.w3.org/International/questions/qa-controls
    val s   = new StringBuilder()
    val len = input.length
    var pos = 0

    while (pos < len) {
      input.charAt(pos) match {
        case '<'          => s.append("&lt;")
        case '>'          => s.append("&gt;")
        case '&'          => s.append("&amp;")
        case '"'          => s.append("&quot;")
        case '\n'         => s.append('\n')
        case '\r'         => s.append('\r')
        case '\t'         => s.append('\t')
        case c if c < ' ' =>
        case c            => s.append(c)
      }
      pos += 1
    }

    s.toString()
  }
}
