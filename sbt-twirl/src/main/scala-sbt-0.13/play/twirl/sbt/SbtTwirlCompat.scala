/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.sbt

import sbt._
import sbt.Keys._
import play.twirl.sbt.Import.TwirlKeys._

object SbtTwirlCompat {
  def watchSourcesSettings:  Seq[Setting[_]] = Seq(
    watchSources in Defaults.ConfigGlobal ++= (sources in compileTemplates).value
  )
}
