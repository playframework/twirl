/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.compiler
package test

import java.io._
import org.specs2.mutable._
import twirl.api.Html

object CompilerSpec extends Specification {

  import Helper._

  val sourceDir = new File("compiler/src/test/resources")
  val generatedDir = new File("compiler/target/test/generated-templates")
  val generatedClasses = new File("compiler/target/test/generated-classes")
  scalax.file.Path(generatedDir).deleteRecursively()
  scalax.file.Path(generatedClasses).deleteRecursively()
  scalax.file.Path(generatedClasses).createDirectory()

  "The twirl compiler" should {

    "compile successfully (real)" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      helper.compile[((String, List[String]) => (Int) => Html)]("real.scala.html", "html.real")("World", List("A", "B"))(4).toString.trim must beLike {
        case html =>
          {
            if (html.contains("<h1>Hello World</h1>") &&
              html.contains("You have 2 items") &&
              html.contains("EA") &&
              html.contains("EB")) ok else ko
          }
      }
    }

    "compile successfully (static)" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      helper.compile[(() => Html)]("static.scala.html", "html.static")().toString.trim must be_==(
        "<h1>It works</h1>")
    }

    "compile successfully (patternMatching)" in {
      val testParam = "12345"
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      helper.compile[((String) => Html)]("patternMatching.scala.html", "html.patternMatching")(testParam).toString.trim must be_==(
        """@test
@test.length
@test.length.toInt

@(test)
@(test.length)
@(test.length + 1)
@(test.+(3))

5 match @test.length""")
    }

    "compile successfully (hello)" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      val hello = helper.compile[((String) => Html)]("hello.scala.html", "html.hello")("World").toString.trim
      hello must be_==("<h1>Hello World!</h1><h1>xml</h1>")
    }

    "compile successfully (set)" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      val set = helper.compile[((collection.immutable.Set[String]) => Html)]("set.scala.html", "html.set")(Set("first","second","third")).toString.trim.replace("\n","").replaceAll("\\s+", "")
      set must be_==("firstsecondthird")
    }

    "fail compilation for error.scala.html" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      helper.compile[(() => Html)]("error.scala.html", "html.error") must throwA[CompilationError].like {
        case CompilationError(_, 2, 12) => ok
        case _ => ko
      }
    }

    "compile templates that have contiguous strings > than 64k" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      val input = (scalax.file.Path(sourceDir) / "long.scala.html").string
      val result = helper.compile[(() => Html)]("long.scala.html", "html.long")().toString
      result.length must_== input.length
      result must_== input
    }

    "allow rendering a template twice" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      val inner = helper.compile[((String, List[String]) => (Int) => Html)]("htmlInner.scala.html", "html.htmlInner")("World", List("A", "B"))(4)

      val outer = helper.compile[Html => Html]("htmlParam.scala.html", "html.htmlParam")

      outer(inner).body must contain("Hello World")
      outer(inner).body must contain("Hello World")
    }
  }

  "StringGrouper" should {
    val beer = "\uD83C\uDF7A"
    val line = "abcde" + beer + "fg"

    "split before a surrogate pair" in {
      StringGrouper(line, 5) must contain("abcde", beer + "fg")
    }

    "not split a surrogate pair" in {
      StringGrouper(line, 6) must contain("abcde" + beer, "fg")
    }

    "split after a surrogate pair" in {
      StringGrouper(line, 7) must contain("abcde" + beer, "fg")
    }
  }

}

object Helper {

  case class CompilationError(message: String, line: Int, column: Int) extends RuntimeException(message)

  class CompilerHelper(sourceDir: File, generatedDir: File, generatedClasses: File) {
    import scala.tools.nsc.Global
    import scala.tools.nsc.Settings
    import scala.tools.nsc.reporters.ConsoleReporter
    import scala.reflect.internal.util.Position
    import scala.collection.mutable

    import java.net._

    val twirlCompiler = TwirlCompiler

    val classloader = new URLClassLoader(Array(generatedClasses.toURI.toURL), Class.forName("twirl.compiler.TwirlCompiler").getClassLoader)

    // A list of the compile errors from the most recent compiler run
    val compileErrors = new mutable.ListBuffer[CompilationError]

    val compiler = {

      def additionalClassPathEntry: Option[String] = Some(
        Class.forName("twirl.compiler.TwirlCompiler").getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).mkString(":"))

      val settings = new Settings
      val scalaObjectSource = Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource

      // is null in Eclipse/OSGI but luckily we don't need it there
      if (scalaObjectSource != null) {
        val compilerPath = Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation
        val libPath = scalaObjectSource.getLocation
        val pathList = List(compilerPath, libPath)
        val origBootclasspath = settings.bootclasspath.value
        settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) mkString File.pathSeparator
        settings.outdir.value = generatedClasses.getAbsolutePath
      }

      val compiler = new Global(settings, new ConsoleReporter(settings) {
        override def printMessage(pos: Position, msg: String) = {
          compileErrors.append(CompilationError(msg, pos.line, pos.point))
        }
      })

      compiler
    }

    def compile[T](templateName: String, className: String): T = {
      val templateFile = new File(sourceDir, templateName)
      val Some(generated) = twirlCompiler.compile(templateFile, sourceDir, generatedDir, "twirl.api.HtmlFormat")

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

      val t = classloader.loadClass(className + "$").getDeclaredField("MODULE$").get(null)

      t.getClass.getDeclaredMethod("f").invoke(t).asInstanceOf[T]
    }
  }
}
