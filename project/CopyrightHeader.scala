/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import de.heikoseeberger.sbtheader.HeaderPlugin
import sbt._

object CopyrightHeader extends AutoPlugin {
  import HeaderPlugin.autoImport._

  override def requires = HeaderPlugin
  override def trigger  = allRequirements

  override def projectSettings =
    Seq(
      headerEmptyLine := false,
      headerLicense   := Some(HeaderLicense.Custom("Copyright (C) Lightbend Inc. <https://www.lightbend.com>"))
    )

}
