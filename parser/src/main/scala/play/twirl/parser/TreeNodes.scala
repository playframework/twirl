/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.parser

import scala.util.parsing.input.Positional

object TreeNodes {
  abstract class TemplateTree
  abstract class ScalaExpPart

  trait LocalMember extends Positional {
    val name: PosString
    val resultType: Option[PosString]
    val code: Simple
  }
  trait BaseTemplate extends Positional {
    val params: PosString
    val imports: collection.Seq[Simple]
    val members: collection.Seq[LocalMember]
    val sub: collection.Seq[SubTemplate]
    val content: collection.Seq[TemplateTree]
  }

  case class Constructor(comment: Option[Comment], params: PosString)
  case class Template(
      constructor: Option[Constructor],
      comment: Option[Comment],
      params: PosString,
      topImports: collection.Seq[Simple],
      imports: collection.Seq[Simple],
      members: collection.Seq[LocalMember],
      sub: collection.Seq[SubTemplate],
      content: collection.Seq[TemplateTree]
  ) extends BaseTemplate
  case class SubTemplate(
      isVal: Boolean,
      isLazy: Boolean, // useless if not isVal
      name: PosString,
      params: PosString,
      imports: collection.Seq[Simple],
      members: collection.Seq[LocalMember],
      sub: collection.Seq[SubTemplate],
      content: collection.Seq[TemplateTree]
  ) extends BaseTemplate
  case class PosString(str: String) extends Positional {
    override def toString: String = str
  }
  case class Def(name: PosString, params: PosString, resultType: Option[PosString], code: Simple) extends LocalMember
  case class Val(name: PosString, isLazy: Boolean, resultType: Option[PosString], code: Simple)   extends LocalMember
  case class Plain(text: String)                           extends TemplateTree with Positional
  case class Display(exp: ScalaExp)                        extends TemplateTree with Positional
  case class Comment(msg: String)                          extends TemplateTree with Positional
  case class ScalaExp(parts: collection.Seq[ScalaExpPart]) extends TemplateTree with Positional
  case class Simple(code: String)                          extends ScalaExpPart with Positional
  case class Block(whitespace: String, args: Option[PosString], content: collection.Seq[TemplateTree])
      extends ScalaExpPart
      with Positional
}
