/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.parser

import scala.util.parsing.input.Positional

object TreeNodes {
  abstract class TemplateTree
  abstract class ScalaExpPart

  case class Params(code: String) extends Positional
  case class Constructor(comment: Option[Comment], params: PosString)
  case class Template(
      name: PosString,
      constructor: Option[Constructor],
      comment: Option[Comment],
      params: PosString,
      topImports: collection.Seq[Simple],
      imports: collection.Seq[Simple],
      defs: collection.Seq[Def],
      sub: collection.Seq[Template],
      content: collection.Seq[TemplateTree]
  ) extends Positional
  case class PosString(str: String) extends Positional {
    override def toString: String = str
  }
  case class Def(name: PosString, params: PosString, code: Simple) extends Positional
  case class Plain(text: String)                                   extends TemplateTree with Positional
  case class Display(exp: ScalaExp)                                extends TemplateTree with Positional
  case class Comment(msg: String)                                  extends TemplateTree with Positional
  case class ScalaExp(parts: collection.Seq[ScalaExpPart])         extends TemplateTree with Positional
  case class Simple(code: String)                                  extends ScalaExpPart with Positional
  case class Block(whitespace: String, args: Option[PosString], content: collection.Seq[TemplateTree])
      extends ScalaExpPart
      with Positional
  case class Value(ident: PosString, block: Block) extends Positional
}
