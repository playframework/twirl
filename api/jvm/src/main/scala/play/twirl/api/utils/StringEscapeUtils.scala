/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.api.utils

import org.apache.commons.lang3

object StringEscapeUtils {

  def escapeEcmaScript(input: String): String = lang3.StringEscapeUtils.escapeEcmaScript(input)

  def escapeXml11(input: String): String = lang3.StringEscapeUtils.escapeXml11(input)

}
