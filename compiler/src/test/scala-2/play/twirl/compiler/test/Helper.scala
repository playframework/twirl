/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.compiler
package test

import java.io._

object Helper {
  case class CompilationError(message: String, line: Int, column: Int) extends RuntimeException(message)

  class CompilerHelper(sourceDir: File, generatedDir: File, generatedClasses: File) {
    import java.net._
    import scala.collection.mutable
    import scala.reflect.internal.util.Position
    import scala.tools.nsc.reporters.ConsoleReporter
    import scala.tools.nsc.Global
    import scala.tools.nsc.Settings

    val twirlCompiler = TwirlCompiler

    val classloader = new URLClassLoader(
      Array(generatedClasses.toURI.toURL),
      Class.forName("play.twirl.compiler.TwirlCompiler").getClassLoader
    )

    // A list of the compile errors from the most recent compiler run
    val compileErrors = new mutable.ListBuffer[CompilationError]

    val compiler = {
      def additionalClassPathEntry: Option[String] =
        Some(
          Class
            .forName("play.twirl.compiler.TwirlCompiler")
            .getClassLoader
            .asInstanceOf[URLClassLoader]
            .getURLs
            .map(url => new File(url.toURI))
            .mkString(":")
        )

      val settings          = new Settings
      val scalaObjectSource = Class.forName("scala.Option").getProtectionDomain.getCodeSource

      // is null in Eclipse/OSGI but luckily we don't need it there
      if (scalaObjectSource != null) {
        val compilerPath = Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation
        val libPath      = scalaObjectSource.getLocation
        val pathList     = List(compilerPath, libPath)
        val origBootclasspath = settings.bootclasspath.value
        settings.bootclasspath.value =
          ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList).mkString(File.pathSeparator)
        settings.outdir.value = generatedClasses.getAbsolutePath
      }

      val compiler = new Global(
        settings,
        new ConsoleReporter(settings) {
          override def display(pos: Position, msg: String, severity: Severity): Unit = {
            pos match {
              case scala.reflect.internal.util.NoPosition => // do nothing
              case _ => compileErrors.append(CompilationError(msg, pos.line, pos.point))
            }
          }
        }
      )

      compiler
    }

    class CompiledTemplate[T](className: String) {
      private def getF(template: Any) = {
        template.getClass.getMethod("f").invoke(template).asInstanceOf[T]
      }

      def static: T = {
        getF(classloader.loadClass(className + "$").getDeclaredField("MODULE$").get(null))
      }

      def inject(constructorArgs: Any*): T = {
        classloader.loadClass(className).getConstructors match {
          case Array(single) => getF(single.newInstance(constructorArgs.asInstanceOf[Seq[AnyRef]]: _*))
          case other =>
            throw new IllegalStateException(className + " does not declare exactly one constructor: " + other)
        }
      }
    }

    def compile[T](
        templateName: String,
        className: String,
        additionalImports: Seq[String] = Nil
    ): CompiledTemplate[T] = {
      val templateFile = new File(sourceDir, templateName)
      val Some(generated) = twirlCompiler.compile(
        templateFile,
        sourceDir,
        generatedDir,
        "play.twirl.api.HtmlFormat",
        additionalImports = TwirlCompiler.DefaultImports ++ additionalImports
      )

      val mapper = GeneratedSource(generated)

      val run = new compiler.Run

      compileErrors.clear()

      run.compile(List(generated.getAbsolutePath))

      compileErrors.headOption.foreach {
        case CompilationError(msg, line, column) => {
          compileErrors.clear()
          throw CompilationError(msg, mapper.mapLine(line), mapper.mapPosition(column))
        }
      }

      new CompiledTemplate[T](className)
    }
  }
}
