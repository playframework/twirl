/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.compiler

import java.io.File
import scala.annotation.tailrec
import scala.io.Codec
import scala.meta.classifiers._
import play.twirl.parser.TwirlIO
import play.twirl.parser.TwirlParser
import scala.util.parsing.input.Position
import scala.util.parsing.input.OffsetPosition
import scala.util.parsing.input.NoPosition

object Hash {
  def apply(bytes: Array[Byte], imports: collection.Seq[String]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    imports.foreach(i => digest.update(i.getBytes("utf-8")))
    digest.digest().map(0xff & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }
}

case class TemplateCompilationError(source: File, message: String, line: Int, column: Int)
    extends RuntimeException(message)

object MaybeGeneratedSource {
  def unapply(source: File): Option[GeneratedSource] = apply(source, TwirlIO.defaultCodec)

  def apply(source: File, codec: Codec): Option[GeneratedSource] = {
    val generated = GeneratedSource(source, codec)
    if (generated.meta.isDefinedAt("SOURCE")) {
      Some(generated)
    } else {
      None
    }
  }
}

sealed trait AbstractGeneratedSource {
  def content: String

  lazy val meta: Map[String, String] = {
    val Meta          = """([A-Z]+): (.*)""".r
    val UndefinedMeta = """([A-Z]+):""".r
    Map.empty[String, String] ++ {
      try {
        content
          .split("-- GENERATED --")(1)
          .trim
          .split('\n')
          .map { m =>
            m.trim match {
              case Meta(key, value)   => key -> value
              case UndefinedMeta(key) => key -> ""
              case _                  => ("UNDEFINED", "")
            }
          }
          .toMap
      } catch {
        case _: Exception => Map.empty[String, String]
      }
    }
  }

  lazy val matrix: Seq[(Int, Int)] = {
    for {
      pos <- meta("MATRIX").split('|').toIndexedSeq
      c = pos.split("->")
    } yield try {
      Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
    } catch {
      case _: Exception => (0, 0) // Skip if MATRIX meta is corrupted
    }
  }

  lazy val lines: Seq[(Int, Int)] = {
    for {
      pos <- meta("LINES").split('|').toIndexedSeq
      c = pos.split("->")
    } yield try {
      Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
    } catch {
      case _: Exception => (0, 0) // Skip if LINES meta is corrupted
    }
  }

  def mapPosition(generatedPosition: Int): Int = {
    matrix.indexWhere(p => p._1 > generatedPosition) match {
      case 0 => 0
      case i if i > 0 => {
        val pos = matrix(i - 1)
        pos._2 + (generatedPosition - pos._1)
      }
      case _ => {
        val pos = matrix.takeRight(1)(0)
        pos._2 + (generatedPosition - pos._1)
      }
    }
  }

  def mapLine(generatedLine: Int): Int = {
    lines.indexWhere(p => p._1 > generatedLine) match {
      case 0 => 0
      case i if i > 0 => {
        val line = lines(i - 1)
        line._2 + (generatedLine - line._1)
      }
      case _ => {
        val line = lines.takeRight(1)(0)
        line._2 + (generatedLine - line._1)
      }
    }
  }
}

case class GeneratedSource(file: File, codec: Codec = TwirlIO.defaultCodec) extends AbstractGeneratedSource {
  def content = TwirlIO.readFileAsString(file, codec)

  def needRecompilation(imports: collection.Seq[String]): Boolean =
    !file.exists ||
      // A generated source already exist but
      source.isDefined && ((source.get.lastModified > file.lastModified) || // the source has been modified
        (meta("HASH") != Hash(TwirlIO.readFile(source.get), imports)))      // or the hash don't match

  def toSourcePosition(marker: Int): (Int, Int) = {
    try {
      val targetMarker = mapPosition(marker)
      val line         = TwirlIO.readFileAsString(source.get, codec).substring(0, targetMarker).split('\n').size
      (line, targetMarker)
    } catch {
      case _: Exception => (0, 0)
    }
  }

  def source: Option[File] = {
    val s = new File(meta("SOURCE"))
    if (s == null || !s.exists) {
      None
    } else {
      Some(s)
    }
  }

  def sync(): Unit = {
    if (file.exists && !source.isDefined) {
      file.delete()
    }
  }
}

case class GeneratedSourceVirtual(path: String) extends AbstractGeneratedSource {
  var _content = ""
  def setContent(newContent: String): Unit = {
    this._content = newContent
  }
  def content = _content
}

object TwirlCompiler {

  // For constants that depend on Scala 2 or 3 mode.
  private[compiler] class ScalaCompat(val emitScala3Sources: Boolean) {
    val varargSplicesSyntax: String =
      if (emitScala3Sources) "*" else ": _*"
    def valueOrEmptyIfScala3Exceeding22Params(params: Int, value: => String): String =
      if (emitScala3Sources && params > 22) "" else value
    val usingSyntax: String =
      if (emitScala3Sources) "using " else ""
  }

  private[compiler] object ScalaCompat {
    def apply(scalaVersion: Option[String]): ScalaCompat =
      new ScalaCompat(scalaVersion.exists(_.startsWith("3.")))
  }

  def defaultImports(scalaVersion: String) = {
    val implicits = if (scalaVersion.startsWith("3.")) {
      Seq(
        "_root_.play.twirl.api.TwirlFeatureImports.*",
        "_root_.play.twirl.api.TwirlHelperImports.*",
        "scala.language.adhocExtensions"
      )
    } else {
      Seq(
        "_root_.play.twirl.api.TwirlFeatureImports._",
        "_root_.play.twirl.api.TwirlHelperImports._",
      )
    }

    val formats =
      Seq(
        "_root_.play.twirl.api.Html",
        "_root_.play.twirl.api.JavaScript",
        "_root_.play.twirl.api.Txt",
        "_root_.play.twirl.api.Xml"
      )

    implicits ++ formats

  }

  import play.twirl.parser.TreeNodes._

  def compile(
      source: File,
      sourceDirectory: File,
      generatedDirectory: File,
      formatterType: String,
      additionalImports: collection.Seq[String] = Nil,
      constructorAnnotations: collection.Seq[String] = Nil,
      codec: Codec = TwirlIO.defaultCodec,
      inclusiveDot: Boolean = false
  ): Option[File] =
    compile(
      source,
      sourceDirectory,
      generatedDirectory,
      formatterType,
      None,
      additionalImports,
      constructorAnnotations,
      codec,
      inclusiveDot
    )

  def compile(
      source: File,
      sourceDirectory: File,
      generatedDirectory: File,
      formatterType: String,
      scalaVersion: Option[String],
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String],
      codec: Codec,
      inclusiveDot: Boolean
  ): Option[File] = {
    val resultType = formatterType + ".Appendable"
    val (templateName, generatedSource) =
      generatedFile(source, codec, sourceDirectory, generatedDirectory, inclusiveDot)
    if (generatedSource.needRecompilation(additionalImports)) {
      val generated = parseAndGenerateCode(
        templateName,
        TwirlIO.readFile(source),
        codec,
        relativePath(source),
        resultType,
        formatterType,
        scalaVersion,
        additionalImports,
        constructorAnnotations,
        inclusiveDot
      )
      TwirlIO.writeStringToFile(generatedSource.file, generated.toString, codec)
      Some(generatedSource.file)
    } else {
      None
    }
  }

  def compileVirtual(
      content: String,
      source: File,
      sourceDirectory: File,
      resultType: String,
      formatterType: String,
      additionalImports: collection.Seq[String] = Nil,
      constructorAnnotations: collection.Seq[String] = Nil,
      codec: Codec = TwirlIO.defaultCodec,
      inclusiveDot: Boolean = false
  ): GeneratedSourceVirtual =
    compileVirtual(
      content,
      source,
      sourceDirectory,
      resultType,
      formatterType,
      None,
      additionalImports,
      constructorAnnotations,
      codec,
      inclusiveDot
    )

  def compileVirtual(
      content: String,
      source: File,
      sourceDirectory: File,
      resultType: String,
      formatterType: String,
      scalaVersion: Option[String],
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String],
      codec: Codec,
      inclusiveDot: Boolean
  ): GeneratedSourceVirtual = {

    val (templateName, generatedSource) = generatedFileVirtual(source, sourceDirectory, inclusiveDot)
    val generated = parseAndGenerateCode(
      templateName,
      content.getBytes(codec.charSet),
      codec,
      relativePath(source),
      resultType,
      formatterType,
      scalaVersion,
      additionalImports,
      constructorAnnotations,
      inclusiveDot
    )
    generatedSource.setContent(generated)
    generatedSource
  }

  private def relativePath(file: File): String =
    new File(".").toURI.relativize(file.toURI).getPath

  def parseAndGenerateCode(
      templateName: Array[String],
      content: Array[Byte],
      codec: Codec,
      relativePath: String,
      resultType: String,
      formatterType: String,
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String],
      inclusiveDot: Boolean
  ): String = parseAndGenerateCode(
    templateName,
    content,
    codec,
    relativePath,
    resultType,
    formatterType,
    None,
    additionalImports,
    constructorAnnotations,
    inclusiveDot
  )

  private def parseAndGenerateCode(
      templateName: Array[String],
      content: Array[Byte],
      codec: Codec,
      relativePath: String,
      resultType: String,
      formatterType: String,
      scalaVersion: Option[String],
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String],
      inclusiveDot: Boolean
  ): String = {
    val templateParser = new TwirlParser(inclusiveDot)
    templateParser.parse(new String(content, codec.charSet)) match {
      case templateParser.Success(parsed: Template, rest) if rest.atEnd() => {
        generateFinalTemplate(
          relativePath,
          content,
          templateName.dropRight(1).mkString("."),
          templateName.takeRight(1).mkString,
          parsed,
          resultType,
          formatterType,
          ScalaCompat(scalaVersion),
          additionalImports,
          constructorAnnotations
        )
      }
      case templateParser.Success(_, rest) => {
        throw new TemplateCompilationError(new File(relativePath), "Not parsed?", rest.pos().line, rest.pos().column)
      }
      case templateParser.Error(_, rest, errors) => {
        val firstError = errors.head
        throw new TemplateCompilationError(
          new File(relativePath),
          firstError.str,
          firstError.pos.line,
          firstError.pos.column
        )
      }
    }
  }

  def generatedFile(
      template: File,
      codec: Codec,
      sourceDirectory: File,
      generatedDirectory: File,
      inclusiveDot: Boolean
  ) = {
    val templateName = {
      val name =
        source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head).split('.')
      if (inclusiveDot) addInclusiveDotName(name) else name
    }
    templateName -> GeneratedSource(new File(generatedDirectory, templateName.mkString("/") + ".template.scala"), codec)
  }

  def generatedFileVirtual(template: File, sourceDirectory: File, inclusiveDot: Boolean) = {
    val templateName = {
      val name =
        source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head).split('.')
      if (inclusiveDot) addInclusiveDotName(name) else name
    }
    templateName -> GeneratedSourceVirtual(templateName.mkString("/") + ".template.scala")
  }

  def addInclusiveDotName(templateName: Array[String]): Array[String] = {
    if (!templateName.isEmpty)
      templateName.patch(templateName.length - 1, List(templateName.last + "$$TwirlInclusiveDot"), 1)
    else
      templateName
  }

  @tailrec
  def source2TemplateName(
      f: File,
      sourceDirectory: File,
      ext: String,
      suffix: String = "",
      topDirectory: String = "views",
      setExt: Boolean = true
  ): String = {
    val Name = """([a-zA-Z0-9_]+)[.]scala[.]([a-z]+)""".r
    (f, f.getName) match {
      case (f, _) if f == sourceDirectory => {
        if (setExt) {
          val parts = suffix.split('.')
          Option(parts.dropRight(1).mkString(".")).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + ext + "." + parts
            .takeRight(1)
            .mkString
        } else suffix
      }
      case (f, name) if name == topDirectory =>
        source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + ext + "." + suffix, topDirectory, false)
      case (f, Name(name, _)) if f.isFile =>
        source2TemplateName(f.getParentFile, sourceDirectory, ext, name, topDirectory, setExt)
      case (f, name) if !f.isFile =>
        source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + suffix, topDirectory, setExt)
      case (f, name) =>
        throw TemplateCompilationError(
          f,
          "Invalid template name [" + name + "], filenames must only consist of alphanumeric characters and underscores or periods.",
          0,
          0
        )
    }
  }

  protected def displayVisitedChildren(children: collection.Seq[Any]): collection.Seq[Any] = {
    children.size match {
      case 0 => Nil
      case 1 => Nil :+ "_display_(" :+ children :+ ")"
      case _ => Nil :+ "_display_(Seq[Any](" :+ children :+ "))"
    }
  }

  private val tripleQuote = "\"" * 3
  // Scala doesn't offer a way to escape triple quoted strings inside triple quoted strings (to my knowledge), so we
  // have to escape them in this rather crude way
  // We need to double escape slashes, since it's a regex replacement
  private val escapedTripleQuote       = "\\\"" * 3
  private val doubleEscapedTripleQuote = "\\\\\"" * 3
  private val tripleQuoteReplacement =
    escapedTripleQuote + " + \\\"" + doubleEscapedTripleQuote + "\\\" + " + escapedTripleQuote
  private def quoteAndEscape(text: String): collection.Seq[String] = {
    Seq(tripleQuote, text.replaceAll(tripleQuote, tripleQuoteReplacement), tripleQuote)
  }

  def visit(elem: collection.Seq[TemplateTree], previous: collection.Seq[Any]): collection.Seq[Any] = {
    elem.toList match {
      case head :: tail =>
        visit(
          tail,
          head match {
            case p @ Plain(text) =>
              // String literals may not be longer than 65536 bytes. They are encoded as UTF-8 in the classfile, each
              // UTF-16 2 byte char could end up becoming up to 3 bytes, so that puts an upper limit of somewhere
              // over 20000 characters. 20000 characters is a nice round number, use that.
              val grouped = StringGrouper(text, 20000)
              (if (previous.isEmpty) Nil else previous :+ ",") :+
                "format.raw" :+ Source("(", p.pos) :+ quoteAndEscape(grouped.head) :+ ")" :+
                grouped.tail.flatMap { t => Seq(",\nformat.raw(", quoteAndEscape(t), ")") }
            case Comment(msg) => previous
            case Display(exp) =>
              (if (previous.isEmpty) Nil else previous :+ ",") :+ displayVisitedChildren(visit(Seq(exp), Nil))
            case ScalaExp(parts) =>
              previous :+ parts.map {
                case s @ Simple(code) => Source(code, s.pos)
                case b @ Block(whitespace, args, content) if content.forall(_.isInstanceOf[ScalaExp]) =>
                  Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ visit(content, Nil) :+ "}"
                case b @ Block(whitespace, args, content) =>
                  Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ displayVisitedChildren(
                    visit(content, Nil)
                  ) :+ "}"
              }
          }
        )
      case Nil => previous
    }
  }

  def templateCode(template: Template, resultType: String): collection.Seq[Any] = {
    val defs = (template.sub ++ template.defs).map {
      case t: Template if t.name.toString == "" => templateCode(t, resultType)
      case t: Template => {
        Nil :+ (if (t.name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(
          t.name.str,
          t.name.pos
        ) :+ Source(
          t.params.str,
          t.params.pos
        ) :+ ":" :+ resultType :+ " = {_display_(" :+ templateCode(t, resultType) :+ ")};"
      }
      case Def(name, params, resultType, block) => {
        Nil :+ (if (name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(
          name.str,
          name.pos
        ) :+ Source(
          params.str,
          params.pos
        ) :+ resultType.map(":" + _.str).getOrElse("") :+ " = {" :+ block.code :+ "};"
      }
    }

    val imports = formatImports(template.imports)

    Nil :+ imports :+ "\n" :+ defs :+ "\n" :+ "Seq[Any](" :+ visit(template.content, Nil) :+ ")"
  }

  def generateCode(
      packageName: String,
      name: String,
      root: Template,
      resultType: String,
      formatterType: String,
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String]
  ): collection.Seq[Any] = generateCode(
    packageName,
    name,
    root,
    resultType,
    formatterType,
    ScalaCompat(None),
    additionalImports,
    constructorAnnotations
  )

  private def generateCode(
      packageName: String,
      name: String,
      root: Template,
      resultType: String,
      formatterType: String,
      scalaCompat: ScalaCompat,
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String]
  ): collection.Seq[Any] = {
    val (renderCall, f, templateType) =
      TemplateAsFunctionCompiler.getFunctionMapping(root.params.str, resultType, scalaCompat)

    // Get the imports that we need to include, filtering out empty imports
    val imports: Seq[Any] = Seq(additionalImports.map(i => Seq("import ", i, "\n")), formatImports(root.topImports))

    val classDeclaration = root.constructor.fold[Seq[Any]](
      Seq("object ", name)
    ) { constructor =>
      Vector.empty :+ "/*" :+ constructor.comment.fold("")(_.msg) :+ """*/
class """ :+ name :+ " " :+ constructorAnnotations :+ " " :+ Source(constructor.params.str, constructor.params.pos)
    }

    val generated = {
      Vector.empty :+ """
package """ :+ packageName :+ """

""" :+ imports :+ """
""" :+ classDeclaration :+ """ extends _root_.play.twirl.api.BaseScalaTemplate[""" :+ resultType :+ """,_root_.play.twirl.api.Format[""" :+ resultType :+ """]](""" :+ formatterType :+ """)""" :+
        (if (templateType.nonEmpty) s" with $templateType" else "") :+ """ {

  /*""" :+ root.comment.map(_.msg).getOrElse("") :+ """*/
  def apply""" :+ Source(root.params.str, root.params.pos) :+ """:""" :+ resultType :+ """ = {
    _display_ {
      {
""" :+ templateCode(root, resultType) :+ """
      }
    }
  }

  """ :+ renderCall :+ """

  """ :+ f :+ """

  def ref: this.type = this

}

"""
    }

    generated
  }

  def formatImports(imports: collection.Seq[Simple]): collection.Seq[Any] = {
    imports.map(i => Seq(Source(i.code, i.pos), "\n"))
  }

  def formatImports(templateImports: Seq[String], extension: String): Seq[String] = {
    templateImports.map(_.replace("%format%", extension))
  }

  def generateFinalTemplate(
      relativePath: String,
      contents: Array[Byte],
      packageName: String,
      name: String,
      root: Template,
      resultType: String,
      formatterType: String,
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String]
  ): String = generateFinalTemplate(
    relativePath,
    contents,
    packageName,
    name,
    root,
    resultType,
    formatterType,
    ScalaCompat(None),
    additionalImports,
    constructorAnnotations
  )

  private def generateFinalTemplate(
      relativePath: String,
      contents: Array[Byte],
      packageName: String,
      name: String,
      root: Template,
      resultType: String,
      formatterType: String,
      scalaCompat: ScalaCompat,
      additionalImports: collection.Seq[String],
      constructorAnnotations: collection.Seq[String]
  ): String = {
    val generated =
      generateCode(
        packageName,
        name,
        root,
        resultType,
        formatterType,
        scalaCompat,
        additionalImports,
        constructorAnnotations
      )

    Source.finalSource(relativePath, contents, generated, Hash(contents, additionalImports))
  }

  object TemplateAsFunctionCompiler {
    import scala.meta._
    import scala.meta.inputs.Input
    import scala.meta.tokens.Tokens
    import scala.meta.parsers.Parse
    import scala.meta.parsers.ParseException

    object ByNameParam {
      def unapply(param: Term.Param): Option[(String, String)] =
        param.decltpe match {
          case Some(t: Type.ByName) => Some((t.toString, t.tpe.toString))
          case _                    => None
        }
    }

    def getFunctionMapping(
        signature: String,
        returnType: String,
    ): (String, String, String) =
      getFunctionMapping(signature, returnType, ScalaCompat(None))

    private[compiler] def getFunctionMapping(
        signature: String,
        returnType: String,
        sc: ScalaCompat
    ): (String, String, String) = {

      val params: List[List[Term.Param]] =
        try {
          val dialect = Dialect.current.withAllowGivenUsing(true)
          val input   = Input.String(s"object FT { def signature$signature }")
          val obj     = implicitly[Parse[Stat]].apply(input, dialect).get.asInstanceOf[Defn.Object]
          val templ   = obj.templ
          val defdef  = templ.body.stats.head.asInstanceOf[Decl.Def]
          defdef.paramClauseGroups.headOption.map(_.paramClauses.map(_.values)).getOrElse(Nil)
        } catch {
          case e: ParseException => Nil
        }

      def filterType(p: Term.Param) =
        if (p.decltpe.get.toString.endsWith("*")) s"Array[${p.decltpe.get.toString}]".replace("*", "")
        else p.decltpe.get.toString

      val functionType = "(" + params
        .map(group =>
          "(" + group
            .map { case p =>
              filterType(p)
            }
            .mkString(",") + ")"
        )
        .mkString(" => ") + " => " + returnType + ")"

      val hasContextParameters =
        params.flatten.exists(_.mods.exists(modifier => modifier.is[Mod.Implicit] || modifier.is[Mod.Using]))

      val applyArgs = {
        params.map { group =>
          val groupStr = "(" + group
            .map { p =>
              p.name.toString + Option(p.decltpe.get.toString)
                .filter(_.endsWith("*"))
                .map(_ => s".toIndexedSeq${sc.varargSplicesSyntax}")
                .getOrElse("")
            }
            .mkString(",") + ")"

          // prepend "using" for Scala 3 context parameters on the last param
          if (sc.emitScala3Sources && hasContextParameters && group == params.last)
            groupStr.replace("(", s"(${sc.usingSyntax}")
          else groupStr
        }.mkString
      }

      val renderCall = "def render%s: %s = apply%s".format(
        "(" + params.flatten
          .map {
            case p @ ByNameParam(_, paramType) => p.name.toString + ":" + paramType
            case p                             => p.name.toString + ":" + filterType(p)
          }
          .mkString(",") + ")",
        returnType,
        applyArgs
      )

      val f = "def f:%s = %s => apply%s".format(
        functionType,
        params.map(group => "(" + group.map(_.name.toString).mkString(",") + ")").mkString(" => "),
        applyArgs
      )

      val templateType = sc.valueOrEmptyIfScala3Exceeding22Params(
        params.flatten.size,
        "_root_.play.twirl.api.Template%s[%s%s]".format(
          params.flatten.size,
          params.flatten
            .map {
              case ByNameParam(_, paramType) => paramType
              case p                         => filterType(p)
            }
            .mkString(","),
          (if (params.flatten.isEmpty) "" else ",") + returnType
        )
      )

      (renderCall, f, templateType)
    }

  }
}

/* ------- */

case class Source(code: String, pos: Position = NoPosition)

object Source {
  import scala.collection.mutable.ListBuffer

  def finalSource(
      relativePath: String,
      contents: Array[Byte],
      generatedTokens: collection.Seq[Any],
      hash: String
  ): String = {
    val scalaCode = new StringBuilder
    val positions = ListBuffer.empty[(Int, Int)]
    val lines     = ListBuffer.empty[(Int, Int)]
    serialize(generatedTokens, scalaCode, positions, lines)
    scalaCode.toString + s"""
              /*
                  -- GENERATED --
                  SOURCE: ${relativePath.replace(File.separator, "/")}
                  HASH: $hash
                  MATRIX: ${positions.map(pos => s"${pos._1}->${pos._2}").mkString("|")}
                  LINES: ${lines.map(line => s"${line._1}->${line._2}").mkString("|")}
                  -- GENERATED --
              */
          """
  }

  private def serialize(
      parts: collection.Seq[Any],
      source: StringBuilder,
      positions: ListBuffer[(Int, Int)],
      lines: ListBuffer[(Int, Int)]
  ): Unit = {
    parts.foreach {
      case s: String => source.append(s)
      case Source(code, pos @ OffsetPosition(_, offset)) => {
        source.append("/*" + pos + "*/")
        positions += (source.length                -> offset)
        lines += (source.toString.split('\n').size -> pos.line)
        source.append(code)
      }
      case Source(code, NoPosition) => source.append(code)
      case s: collection.Seq[any]   => serialize(s, source, positions, lines)
    }
  }
}

/**
 * Groups sub sections of Strings. Basically implements String.grouped, except that it guarantees that it won't break
 * surrogate pairs.
 */
object StringGrouper {

  /**
   * Group the given string by the given size.
   *
   * @param s
   *   The string to group.
   * @param n
   *   The size of the groups.
   * @return
   *   A list of strings, grouped by the specific size.
   */
  def apply(s: String, n: Int): List[String] = {
    if (s.length <= n + 1 /* because we'll split at n + 1 if character n - 1 is a high surrogate */ ) {
      List(s)
    } else {
      val parts = if (s.charAt(n - 1).isHighSurrogate) {
        s.splitAt(n + 1)
      } else {
        s.splitAt(n)
      }
      parts._1 :: apply(parts._2, n)
    }
  }
}
