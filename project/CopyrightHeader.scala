/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.LineCommentCreator
import sbt._

object CopyrightHeader extends AutoPlugin {
  import HeaderPlugin.autoImport._

  override def requires = HeaderPlugin
  override def trigger  = allRequirements

  override def projectSettings =
    Seq(
      headerLicense := Some(
        HeaderLicense.Custom(
          "Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
        )
      ),
      headerMappings ++= Map(
        FileType("sbt")        -> HeaderCommentStyle.cppStyleLineComment,
        FileType("properties") -> HeaderCommentStyle.hashLineComment,
        FileType("md")   -> CommentStyle(new LineCommentCreator("<!---", "-->"), commentBetween("<!---", "*", "-->")),
        FileType("html") -> HeaderCommentStyle.twirlStyleFramedBlockComment
      ),
    )

}
