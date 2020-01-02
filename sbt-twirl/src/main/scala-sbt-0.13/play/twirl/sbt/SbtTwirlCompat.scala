/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
