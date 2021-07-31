/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.compiler

object FunctionCompiler {
  // Note, the presentation compiler is not thread safe, all access to it must be synchronized.  If access to it
  // is not synchronized, then weird things happen like FreshRunReq exceptions are thrown when multiple sub projects
  // are compiled (done in parallel by default by SBT).  So if adding any new methods to this object, make sure you
  // make them synchronized.

  import java.io.File
  import scala.tools.nsc.interactive.Global
  import scala.reflect.internal.Flags
  import scala.reflect.internal.util.SourceFile
  import scala.reflect.internal.util.Position
  import scala.reflect.internal.util.BatchSourceFile
  import scala.tools.nsc.Settings
  import scala.tools.nsc.reporters.ConsoleReporter

  type Tree    = PresentationCompiler.global.Tree
  type DefDef  = PresentationCompiler.global.DefDef
  type TypeDef = PresentationCompiler.global.TypeDef
  type ValDef  = PresentationCompiler.global.ValDef

  // For some reason they got rid of mods.isByNameParam
  object ByNameParam {
    def unapply(param: ValDef): Option[(String, String)] =
      if (param.mods.hasFlag(Flags.BYNAMEPARAM)) {
        Some((param.name.toString, param.tpt.children(1).toString))
      } else None
  }

  /** The maximum time in milliseconds to wait for a compiler response to finish. */
  private val Timeout = 10000

  def getFunctionMapping(signature: String, returnType: String): (String, String, String) =
    synchronized {
      def filterType(t: String) =
        t.replace("_root_.scala.<repeated>", "Array")
          .replace("<synthetic>", "")

      def findSignature(tree: Tree): Option[DefDef] = {
        tree match {
          case t: DefDef if t.name.toString == "signature" => Some(t)
          case t: Tree                                     => t.children.flatMap(findSignature).headOption
        }
      }

      val params =
        findSignature(PresentationCompiler.treeFrom("object FT { def signature" + signature + " }")).get.vparamss

      val resp = PresentationCompiler.global
        .askForResponse { () =>
          val functionType = "(" + params
            .map(group =>
              "(" + group
                .map {
                  case ByNameParam(_, paramType) => " => " + paramType
                  case a                         => filterType(a.tpt.toString)
                }
                .mkString(",") + ")"
            )
            .mkString(" => ") + " => " + returnType + ")"

          val renderCall = "def render%s: %s = apply%s".format(
            "(" + params.flatten
              .map {
                case ByNameParam(name, paramType) => name + ":" + paramType
                case a                            => a.name.toString + ":" + filterType(a.tpt.toString)
              }
              .mkString(",") + ")",
            returnType,
            params
              .map(group =>
                "(" + group
                  .map { p =>
                    p.name.toString + Option(p.tpt.toString)
                      .filter(_.startsWith("_root_.scala.<repeated>"))
                      .map(_ => ".toIndexedSeq:_*")
                      .getOrElse("")
                  }
                  .mkString(",") + ")"
              )
              .mkString
          )

          val templateType = "_root_.play.twirl.api.Template%s[%s%s]".format(
            params.flatten.size,
            params.flatten
              .map {
                case ByNameParam(_, paramType) => paramType
                case a                         => filterType(a.tpt.toString)
              }
              .mkString(","),
            (if (params.flatten.isEmpty) "" else ",") + returnType
          )

          val f = "def f:%s = %s => apply%s".format(
            functionType,
            params.map(group => "(" + group.map(_.name.toString).mkString(",") + ")").mkString(" => "),
            params
              .map(group =>
                "(" + group
                  .map { p =>
                    p.name.toString + Option(p.tpt.toString)
                      .filter(_.startsWith("_root_.scala.<repeated>"))
                      .map(_ => ".toIndexedSeq:_*")
                      .getOrElse("")
                  }
                  .mkString(",") + ")"
              )
              .mkString
          )

          (renderCall, f, templateType)
        }
        .get(Timeout)

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
        def urlToFile(url: URL): File =
          try {
            val file = new File(url.toURI)
            if (file.exists) file else new File(url.getPath) // assume malformed URL
          } catch {
            case _: java.net.URISyntaxException =>
              // malformed URL: fallback to using the URL path directly
              new File(url.getPath)
          }
        def toAbsolutePath(cs: CodeSource): String = urlToFile(cs.getLocation).getAbsolutePath
        val compilerPath = toAbsolutePath(
          Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource
        )
        val libPath           = toAbsolutePath(scalaPredefSource)
        val pathList          = List(compilerPath, libPath)
        val origBootclasspath = settings.bootclasspath.value
        settings.bootclasspath.value =
          ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList).mkString(File.pathSeparator)
      }

      val compiler = new Global(
        settings,
        new ConsoleReporter(settings) {
          override def display(pos: Position, msg: String, severity: Severity): Unit = ()
        }
      )

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

    def shutdown(): Unit = {
      global.askShutdown()
    }
  }
}
