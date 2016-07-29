/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.api.utils

object StringEscapeUtils {

  def escapeEcmaScript(input: String): String = ???

  def escapeXml11(input: String): String = throw new RuntimeException("XML is not Supported on ScalaJS")

}
