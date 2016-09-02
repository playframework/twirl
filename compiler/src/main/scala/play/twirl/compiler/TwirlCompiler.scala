/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.compiler

import java.io.File
import scala.annotation.tailrec
import scala.io.Codec
import scala.reflect.internal.Flags
import play.twirl.parser.{TwirlIO, TwirlParser}

object Hash {

  def apply(bytes: Array[Byte], imports: Seq[String]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    imports.foreach(i => digest.update(i.getBytes("utf-8")))
    digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }

}

case class TemplateCompilationError(source: File, message: String, line: Int, column: Int) extends RuntimeException(message)

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
    val Meta = """([A-Z]+): (.*)""".r
    val UndefinedMeta = """([A-Z]+):""".r
    Map.empty[String, String] ++ {
      try {
        content.split("-- GENERATED --")(1).trim.split('\n').map { m =>
          m.trim match {
            case Meta(key, value) => (key -> value)
            case UndefinedMeta(key) => (key -> "")
            case _ => ("UNDEFINED", "")
          }
        }.toMap
      } catch {
        case _: Exception => Map.empty[String, String]
      }
    }
  }

  lazy val matrix: Seq[(Int, Int)] = {
    for (pos <- meta("MATRIX").split('|'); c = pos.split("->"))
      yield try {
      Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
    } catch {
      case _: Exception => (0, 0) // Skip if MATRIX meta is corrupted
    }
  }

  lazy val lines: Seq[(Int, Int)] = {
    for (pos <- meta("LINES").split('|'); c = pos.split("->"))
      yield try {
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

  def needRecompilation(imports: Seq[String]): Boolean = !file.exists ||
    // A generated source already exist but
    source.isDefined && ((source.get.lastModified > file.lastModified) || // the source has been modified
      (meta("HASH") != Hash(TwirlIO.readFile(source.get), imports))) // or the hash don't match

  def toSourcePosition(marker: Int): (Int, Int) = {
    try {
      val targetMarker = mapPosition(marker)
      val line = TwirlIO.readFileAsString(source.get, codec).substring(0, targetMarker).split('\n').size
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

  def sync() {
    if (file.exists && !source.isDefined) {
      file.delete()
    }
  }

}

case class GeneratedSourceVirtual(path: String) extends AbstractGeneratedSource {
  var _content = ""
  def setContent(newContent: String) {
    this._content = newContent
  }
  def content = _content
}

object TwirlCompiler {

  val DefaultImports = Seq(
    "_root_.play.twirl.api.TwirlFeatureImports._",
    "_root_.play.twirl.api.TwirlHelperImports._",
    "_root_.play.twirl.api.Html",
    "_root_.play.twirl.api.JavaScript",
    "_root_.play.twirl.api.Txt",
    "_root_.play.twirl.api.Xml"
  )

  import play.twirl.parser.TreeNodes._

  def compile(source: File, sourceDirectory: File, generatedDirectory: File, formatterType: String,
    additionalImports: Seq[String] = Nil, constructorAnnotations: Seq[String] = Nil, codec: Codec = TwirlIO.defaultCodec,
    inclusiveDot: Boolean = false) = {
    val resultType = formatterType + ".Appendable"
    val (templateName, generatedSource) = generatedFile(source, codec, sourceDirectory, generatedDirectory, inclusiveDot)
    if (generatedSource.needRecompilation(additionalImports)) {
      val generated = parseAndGenerateCode(templateName, TwirlIO.readFile(source), codec, source.getAbsolutePath,
        resultType, formatterType, additionalImports, constructorAnnotations, inclusiveDot)
      TwirlIO.writeStringToFile(generatedSource.file, generated.toString, codec)
      Some(generatedSource.file)
    } else {
      None
    }
  }

  def compileVirtual(content: String, source: File, sourceDirectory: File, resultType: String, formatterType: String,
    additionalImports: Seq[String] = Nil, constructorAnnotations: Seq[String] = Nil,
    codec: Codec = TwirlIO.defaultCodec, inclusiveDot: Boolean = false) = {
    val (templateName, generatedSource) = generatedFileVirtual(source, sourceDirectory, inclusiveDot)
    val generated = parseAndGenerateCode(templateName, content.getBytes(codec.charSet), codec, source.getAbsolutePath,
      resultType, formatterType, additionalImports, constructorAnnotations, inclusiveDot)
    generatedSource.setContent(generated)
    generatedSource
  }

  def parseAndGenerateCode(templateName: Array[String], content: Array[Byte], codec: Codec, absolutePath: String,
    resultType: String, formatterType: String, additionalImports: Seq[String], constructorAnnotations: Seq[String],
    inclusiveDot: Boolean) = {
    val templateParser = new TwirlParser(inclusiveDot)
    templateParser.parse(new String(content, codec.charSet)) match {
      case templateParser.Success(parsed: Template, rest) if rest.atEnd => {
        generateFinalTemplate(absolutePath,
          content,
          templateName.dropRight(1).mkString("."),
          templateName.takeRight(1).mkString,
          parsed,
          resultType,
          formatterType,
          additionalImports,
          constructorAnnotations
        )
      }
      case templateParser.Success(_, rest) => {
        throw new TemplateCompilationError(new File(absolutePath), "Not parsed?", rest.pos.line, rest.pos.column)
      }
      case templateParser.Error(_, rest, errors) => {
        val firstError = errors.head
        throw new TemplateCompilationError(new File(absolutePath), firstError.str, firstError.pos.line, firstError.pos.column)
      }
    }
  }

  def generatedFile(template: File, codec: Codec, sourceDirectory: File, generatedDirectory: File, inclusiveDot: Boolean) = {
    val templateName = {
      val name = source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head).split('.')
      if (inclusiveDot) addInclusiveDotName(name) else name
    }
    templateName -> GeneratedSource(new File(generatedDirectory, templateName.mkString("/") + ".template.scala"), codec)
  }

  def generatedFileVirtual(template: File, sourceDirectory: File, inclusiveDot: Boolean) = {
    val templateName = {
      val name = source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head).split('.')
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
  def source2TemplateName(f: File, sourceDirectory: File, ext: String, suffix: String = "", topDirectory: String = "views", setExt: Boolean = true): String = {
    val Name = """([a-zA-Z0-9_]+)[.]scala[.]([a-z]+)""".r
    (f, f.getName) match {
      case (f, _) if f == sourceDirectory => {
        if (setExt) {
          val parts = suffix.split('.')
          Option(parts.dropRight(1).mkString(".")).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + ext + "." + parts.takeRight(1).mkString
        } else suffix
      }
      case (f, name) if name == topDirectory => source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + ext + "." + suffix, topDirectory, false)
      case (f, Name(name, _)) if f.isFile => source2TemplateName(f.getParentFile, sourceDirectory, ext, name, topDirectory, setExt)
      case (f, name) if !f.isFile => source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + suffix, topDirectory, setExt)
      case (f, name) => throw TemplateCompilationError(f, "Invalid template name [" + name + "], filenames must only consist of alphanumeric characters and underscores or periods.", 0, 0)
    }
  }

  protected def displayVisitedChildren(children: Seq[Any]): Seq[Any] = {
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
  private val escapedTripleQuote = "\\\"" * 3
  private val doubleEscapedTripleQuote = "\\\\\"" * 3
  private val tripleQuoteReplacement = escapedTripleQuote + " + \\\"" + doubleEscapedTripleQuote + "\\\" + " + escapedTripleQuote
  private def quoteAndEscape(text: String): Seq[String] = {
    Seq(tripleQuote, text.replaceAll(tripleQuote, tripleQuoteReplacement), tripleQuote)
  }

  def visit(elem: Seq[TemplateTree], previous: Seq[Any]): Seq[Any] = {
    elem.toList match {
      case head :: tail =>
        visit(tail, head match {
          case p @ Plain(text) =>

            // String literals may not be longer than 65536 bytes. They are encoded as UTF-8 in the classfile, each
            // UTF-16 2 byte char could end up becoming up to 3 bytes, so that puts an upper limit of somewhere
            // over 20000 characters. 20000 characters is a nice round number, use that.
            val grouped = StringGrouper(text, 20000)
            (if (previous.isEmpty) Nil else previous :+ ",") :+
              "format.raw" :+ Source("(", p.pos) :+ quoteAndEscape(grouped.head) :+ ")" :+
              grouped.tail.flatMap { t => Seq(",\nformat.raw(", quoteAndEscape(t), ")") }
          case Comment(msg) => previous
          case Display(exp) => (if (previous.isEmpty) Nil else previous :+ ",") :+ displayVisitedChildren(visit(Seq(exp), Nil))
          case ScalaExp(parts) => previous :+ parts.map {
            case s @ Simple(code) => Source(code, s.pos)
            case b @ Block(whitespace, args, content) if (content.forall(_.isInstanceOf[ScalaExp])) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ visit(content, Nil) :+ "}"
            case b @ Block(whitespace, args, content) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ displayVisitedChildren(visit(content, Nil)) :+ "}"
          }
        })
      case Nil => previous
    }
  }

  def templateCode(template: Template, resultType: String): Seq[Any] = {

    val defs = (template.sub ++ template.defs).map { i =>
      i match {
        case t: Template if t.name == "" => templateCode(t, resultType)
        case t: Template => {
          Nil :+ (if (t.name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(t.name.str, t.name.pos) :+ Source(t.params.str, t.params.pos) :+ ":" :+ resultType :+ " = {_display_(" :+ templateCode(t, resultType) :+ ")};"
        }
        case Def(name, params, block) => {
          Nil :+ (if (name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(name.str, name.pos) :+ Source(params.str, params.pos) :+ " = {" :+ block.code :+ "};"
        }
      }
    }

    val imports = formatImports(template.imports)

    Nil :+ imports :+ "\n" :+ defs :+ "\n" :+ "Seq[Any](" :+ visit(template.content, Nil) :+ ")"
  }

  def generateCode(packageName: String, name: String, root: Template, resultType: String, formatterType: String,
    additionalImports: Seq[String], constructorAnnotations: Seq[String]): Seq[Any] = {
    val (renderCall, f, templateType) = TemplateAsFunctionCompiler.getFunctionMapping(
      root.params.str,
      resultType)

    // Get the imports that we need to include, filtering out empty imports
    val imports: Seq[Any] = Seq(additionalImports.map(i => Seq("import ", i, "\n")),
      formatImports(root.topImports))

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
""" :+ classDeclaration :+ """ extends _root_.play.twirl.api.BaseScalaTemplate[""" :+ resultType :+ """,_root_.play.twirl.api.Format[""" :+ resultType :+ """]](""" :+ formatterType :+ """) with """ :+ templateType :+ """ {

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

  def formatImports(imports: Seq[Simple]): Seq[Any] = {
    imports.map(i => Seq(Source(i.code, i.pos), "\n"))
  }

  def generateFinalTemplate(absolutePath: String, contents: Array[Byte], packageName: String, name: String,
    root: Template, resultType: String, formatterType: String, additionalImports: Seq[String],
    constructorAnnotations: Seq[String]): String = {
    val generated = generateCode(packageName, name, root, resultType, formatterType, additionalImports,
      constructorAnnotations)

    Source.finalSource(absolutePath, contents, generated, Hash(contents, additionalImports))
  }

  object TemplateAsFunctionCompiler {

    // Note, the presentation compiler is not thread safe, all access to it must be synchronized.  If access to it
    // is not synchronized, then weird things happen like FreshRunReq exceptions are thrown when multiple sub projects
    // are compiled (done in parallel by default by SBT).  So if adding any new methods to this object, make sure you
    // make them synchronized.

    import java.io.File
    import scala.tools.nsc.interactive.{ Response, Global }
    import scala.tools.nsc.io.AbstractFile
    import scala.reflect.internal.util.{ SourceFile, Position, BatchSourceFile }
    import scala.tools.nsc.Settings
    import scala.tools.nsc.reporters.ConsoleReporter

    type Tree = PresentationCompiler.global.Tree
    type DefDef = PresentationCompiler.global.DefDef
    type TypeDef = PresentationCompiler.global.TypeDef
    type ValDef = PresentationCompiler.global.ValDef

    // For some reason they got rid of mods.isByNameParam
    object ByNameParam {
      def unapply(param: ValDef): Option[(String, String)] = if (param.mods.hasFlag(Flags.BYNAMEPARAM)) {
        Some((param.name.toString, param.tpt.children(1).toString))
      } else None
    }

    /** The maximum time in milliseconds to wait for a compiler response to finish. */
    private val Timeout = 10000

    def getFunctionMapping(signature: String, returnType: String): (String, String, String) = synchronized {

      def filterType(t: String) = t
        .replace("_root_.scala.<repeated>", "Array")
        .replace("<synthetic>", "")

      def findSignature(tree: Tree): Option[DefDef] = {
        tree match {
          case t: DefDef if t.name.toString == "signature" => Some(t)
          case t: Tree => t.children.flatMap(findSignature).headOption
        }
      }

      val params = findSignature(
        PresentationCompiler.treeFrom("object FT { def signature" + signature + " }")).get.vparamss

      val resp = PresentationCompiler.global.askForResponse { () =>

        val functionType = "(" + params.map(group => "(" + group.map {
          case ByNameParam(_, paramType) => " => " + paramType
          case a => filterType(a.tpt.toString)
        }.mkString(",") + ")").mkString(" => ") + " => " + returnType + ")"

        val renderCall = "def render%s: %s = apply%s".format(
          "(" + params.flatten.map {
            case ByNameParam(name, paramType) => name + ":" + paramType
            case a => a.name.toString + ":" + filterType(a.tpt.toString)
          }.mkString(",") + ")",
          returnType,
          params.map(group => "(" + group.map { p =>
            p.name.toString + Option(p.tpt.toString).filter(_.startsWith("_root_.scala.<repeated>")).map(_ => ":_*").getOrElse("")
          }.mkString(",") + ")").mkString)

        val templateType = "_root_.play.twirl.api.Template%s[%s%s]".format(
          params.flatten.size,
          params.flatten.map {
            case ByNameParam(_, paramType) => paramType
            case a => filterType(a.tpt.toString)
          }.mkString(","),
          (if (params.flatten.isEmpty) "" else ",") + returnType)

        val f = "def f:%s = %s => apply%s".format(
          functionType,
          params.map(group => "(" + group.map(_.name.toString).mkString(",") + ")").mkString(" => "),
          params.map(group => "(" + group.map { p =>
            p.name.toString + Option(p.tpt.toString).filter(_.startsWith("_root_.scala.<repeated>")).map(_ => ":_*").getOrElse("")
          }.mkString(",") + ")").mkString)

        (renderCall, f, templateType)
      }.get(Timeout)

      resp match {
        case None =>
          PresentationCompiler.global.reportThrowable(new Throwable("Timeout in getFunctionMapping"))
          ("", "", "")
        case Some(res) =>
          res match {
            case Right(t) =>
              PresentationCompiler.global.reportThrowable(new Throwable("Throwable in getFunctionMapping", t))
              ("", "", "")
            case Left(res) =>
              res
          }
      }
    }

    class CompilerInstance {

      def additionalClassPathEntry: Option[String] = None

      lazy val compiler = {

        val settings = new Settings

        val scalaPredefSource = Class.forName("scala.Predef").getProtectionDomain.getCodeSource

        // is null in Eclipse/OSGI but luckily we don't need it there
        if (scalaPredefSource != null) {
          import java.net.URL
          import java.security.CodeSource
          def urlToFile(url: URL): File = try {
            val file = new File(url.toURI)
            if (file.exists) file else new File(url.getPath) // assume malformed URL
          } catch {
            case _: java.net.URISyntaxException =>
              // malformed URL: fallback to using the URL path directly
              new File(url.getPath)
          }
          def toAbsolutePath(cs: CodeSource): String = urlToFile(cs.getLocation).getAbsolutePath
          val compilerPath = toAbsolutePath(Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource)
          val libPath = toAbsolutePath(scalaPredefSource)
          val pathList = List(compilerPath, libPath)
          val origBootclasspath = settings.bootclasspath.value
          settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) mkString File.pathSeparator
        }

        val compiler = new Global(settings, new ConsoleReporter(settings) {
          override def printMessage(pos: Position, msg: String) = ()
        })

        // Everything must be done on the compiler thread, because the presentation compiler is a fussy piece of work.
        compiler.ask(() => new compiler.Run)

        compiler
      }
    }

    trait TreeCreationMethods {

      val global: scala.tools.nsc.interactive.Global

      val randomFileName = {
        val r = new java.util.Random
        () => "file" + r.nextInt
      }

      def treeFrom(src: String): global.Tree = {
        val file = new BatchSourceFile(randomFileName(), src)
        treeFrom(file)
      }

      def treeFrom(file: SourceFile): global.Tree = {
        import tools.nsc.interactive.Response
        val r1 = new Response[global.Tree]
        global.askParsedEntered(file, true, r1)
        r1.get match {
          case Left(result) =>
            result
          case Right(error) =>
            throw error
        }
      }

    }

    object CompilerInstance extends CompilerInstance

    object PresentationCompiler extends TreeCreationMethods {
      val global = CompilerInstance.compiler

      def shutdown() {
        global.askShutdown()
      }
    }

  }

}

/* ------- */

import scala.util.parsing.input.{ Position, OffsetPosition, NoPosition }

case class Source(code: String, pos: Position = NoPosition)

object Source {

  import scala.collection.mutable.ListBuffer

  def finalSource(absolutePath: String, contents: Array[Byte], generatedTokens: Seq[Any], hash: String): String = {
    val scalaCode = new StringBuilder
    val positions = ListBuffer.empty[(Int, Int)]
    val lines = ListBuffer.empty[(Int, Int)]
    serialize(generatedTokens, scalaCode, positions, lines)
    scalaCode + """
              /*
                  -- GENERATED --
                  DATE: """ + new java.util.Date + """
                  SOURCE: """ + absolutePath.replace(File.separator, "/") + """
                  HASH: """ + hash + """
                  MATRIX: """ + positions.map { pos =>
      pos._1 + "->" + pos._2
    }.mkString("|") + """
                  LINES: """ + lines.map { line =>
      line._1 + "->" + line._2
    }.mkString("|") + """
                  -- GENERATED --
              */
          """
  }

  private def serialize(parts: Seq[Any], source: StringBuilder, positions: ListBuffer[(Int, Int)], lines: ListBuffer[(Int, Int)]) {
    parts.foreach {
      case s: String => source.append(s)
      case Source(code, pos @ OffsetPosition(_, offset)) => {
        source.append("/*" + pos + "*/")
        positions += (source.length -> offset)
        lines += (source.toString.split('\n').size -> pos.line)
        source.append(code)
      }
      case Source(code, NoPosition) => source.append(code)
      case s: Seq[any] => serialize(s, source, positions, lines)
    }
  }

}

/**
 * Groups sub sections of Strings.  Basically implements String.grouped, except that it guarantees that it won't break
 * surrogate pairs.
 */
object StringGrouper {

  /**
   * Group the given string by the given size.
   *
   * @param s The string to group.
   * @param n The size of the groups.
   * @return A list of strings, grouped by the specific size.
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
