/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.compiler
package test

import java.io._
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import dotty.tools.dotc.core.Contexts
import Contexts.Context
import Contexts.ctx
import dotty.tools.dotc.Compiler
import dotty.tools.dotc.Driver
import dotty.tools.dotc.reporting.ConsoleReporter
import dotty.tools.dotc.reporting.Reporter
import dotty.tools.io.PlainDirectory
import dotty.tools.io.Directory
import dotty.tools.io.ClassPath
import scala.jdk.CollectionConverters._

object Helper {
  case class CompilationError(message: String, line: Int, column: Int) extends RuntimeException(message)

  class CompilerHelper(sourceDir: File, generatedDir: File, generatedClasses: File) {
    import java.net._
    import scala.collection.mutable

    val twirlCompiler = TwirlCompiler

    val classloader = new URLClassLoader(
      Array(generatedClasses.toURI.toURL),
      Class.forName("play.twirl.compiler.TwirlCompiler").getClassLoader,
    )

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
      val scalaVersion = scala.util.Properties.versionNumberString
      val templateFile = new File(sourceDir, templateName)
      val Some(generated) = twirlCompiler.compile(
        templateFile,
        sourceDir,
        generatedDir,
        "play.twirl.api.HtmlFormat",
        additionalImports = TwirlCompiler.defaultImports(scalaVersion) ++ additionalImports
      )

      val mapper = GeneratedSource(generated)

      val compilerArgs = Array(
        "-classpath",
        (Class.forName("play.twirl.compiler.TwirlCompiler").getClassLoader.asInstanceOf[URLClassLoader].getURLs ++
          Class.forName("scala.Tuple").getClassLoader.asInstanceOf[URLClassLoader].getURLs)
          .map(url => new File(url.toURI))
          .mkString(":")
      )

      val driver = new TestDriver(generatedClasses.toPath, compilerArgs, generated.toPath)

      val reporter = driver.compile()

      if (reporter.hasErrors) {
        val error   = reporter.allErrors.sortBy(_.pos.point).head
        val message = error.msg
        val pos     = error.pos
        throw CompilationError(message.toString, mapper.mapLine(pos.line + 1), mapper.mapPosition(pos.point))
      }

      new CompiledTemplate[T](className)
    }

    class TestDriver(outDir: Path, compilerArgs: Array[String], path: Path) extends Driver {
      def compile(): Reporter = {
        val Some((toCompile, rootCtx)) = setup(compilerArgs :+ path.toAbsolutePath.toString, initCtx.fresh)

        val silentReporter = new ConsoleReporter.AbstractConsoleReporter {
          def printMessage(msg: String): Unit = {
            // We want to suppress compile errors log.
            //
            // Some of Twirl's tests verify that the template cannot be compiled.
            // It would be confusing to the developer if a compile error message were displayed at that time.
          }
        }

        given Context = rootCtx.fresh
          .setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir)))
          .setReporter(silentReporter)

        doCompile(newCompiler, toCompile)
      }
    }
  }
}
