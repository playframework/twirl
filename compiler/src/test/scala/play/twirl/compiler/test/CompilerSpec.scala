/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.compiler
package test

import java.io._

import org.scalatest.{ MustMatchers, WordSpec }
import play.twirl.api.Html
import play.twirl.parser.TwirlIO

class CompilerSpec extends WordSpec with MustMatchers {

  import Helper._

  val testName = "Twirl compiler"

  val sourceDir = new File("compiler/src/test/resources")

  def newCompilerHelper = {
    val dirName = "twirl-parser"
    val generatedDir = new File("compiler/target/test/" + dirName + "/generated-templates")
    val generatedClasses = new File("compiler/target/test/" + dirName + "/generated-classes")
    TwirlIO.deleteRecursively(generatedDir)
    TwirlIO.deleteRecursively(generatedClasses)
    generatedClasses.mkdirs()
    new CompilerHelper(sourceDir, generatedDir, generatedClasses)
  }

  testName should {

    "compile successfully (real)" in {
      val helper = newCompilerHelper
      val tmpl = helper.compile[((String, List[String]) => (Int) => Html)]("real.scala.html", "html.real")
          .static("World", List("A", "B"))(4).toString.trim

      tmpl must (include("<h1>Hello World</h1>") and include("You have 2 items") and include("EA") and include("EB"))
    }

    "compile successfully (static)" in {
      val helper = newCompilerHelper
      helper.compile[(() => Html)]("static.scala.html", "html.static").static().toString.trim must be(
        "<h1>It works</h1>")
    }

    "compile successfully (patternMatching)" in {
      val testParam = "12345"
      val helper = newCompilerHelper
      helper.compile[((String) => Html)]("patternMatching.scala.html", "html.patternMatching").static(testParam).toString.trim must be(
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
      val helper = newCompilerHelper
      val hello = helper.compile[((String) => Html)]("hello.scala.html", "html.hello").static("World").toString.trim
      hello must be("<h1>Hello World!</h1><h1>xml</h1>")
    }

    "compile successfully (helloNull)" in {
      val helper = newCompilerHelper
      val hello = helper.compile[((String) => Html)]("helloNull.scala.html", "html.helloNull").static(null).toString.trim
      hello must be("<h1>Hello !</h1>")
    }

    "compile successfully (zipWithIndex)" in {
      val helper = newCompilerHelper
      val output = helper.compile[((Seq[String]) => Html)]("zipWithIndex.scala.html", "html.zipWithIndex").static(Seq("Alice", "Bob", "Charlie")).toString.trim.replace("\n", "")
      output must be("<h1>0 Hello Alice!</h1><h1>1 Hello Bob!</h1><h1>2 Hello Charlie!</h1>")
    }

    "compile successfully (set)" in {
      val helper = newCompilerHelper
      val set = helper.compile[((collection.immutable.Set[String]) => Html)]("set.scala.html", "html.set").static(Set("first", "second", "third")).toString.trim.replace("\n", "").replaceAll("\\s+", "")
      set must be("firstsecondthird")
    }

    "compile successfully (arg imports)" in {
      val helper = newCompilerHelper
      val result = helper.compile[((java.io.File, java.net.URL) => Html)]("argImports.scala.html", "html.argImports").static(new java.io.File("example"), new java.net.URL("http://example.org")).toString.trim
      result must be("<p>file: example, url: http://example.org</p>")
    }

    "compile successfully (default imports)" in {
      val helper = newCompilerHelper
      val result = helper.compile[(() => Html)]("importsDefault.scala.html", "html.importsDefault").static().toString.trim
      result must be("foo")
    }

    "compile successfully (escape closing brace)" in {
      val helper = newCompilerHelper
      val result = helper.compile[(Option[String] => Html)]("escapebrace.scala.html", "html.escapebrace").static(Some("foo")).toString.trim
      result must be("foo: }")
    }

    "compile successfully (utf8)" in {
      val helper = newCompilerHelper
      val text = helper.compile[(() => Html)]("utf8.scala.html", "html.utf8").static().toString.trim
      text must be("€, ö, or ü")
    }

    "compile successfully (existential)" in {
      val helper = newCompilerHelper
      val text = helper.compile[(List[_] => Html)]("existential.scala.html", "html.existential").static(List(1, 2, 3)).toString.trim
      text must be("123")
    }

    "compile successfully (triple quotes)" in {
      val helper = newCompilerHelper
      val out = helper.compile[(() => Html)]("triplequotes.scala.html", "html.triplequotes").static().toString.trim
      out must be("\"\"\"\n\n\"\"\"\"\"\n\n\"\"\"\"\"\"")
    }

    "compile successfully (var args existential)" in {
      val helper = newCompilerHelper
      val text = helper.compile[(Array[List[_]] => Html)]("varArgsExistential.scala.html", "html.varArgsExistential").static(Array(List(1, 2, 3), List(4, 5, 6))).toString.trim
      text must be("123456")
    }

    "fail compilation for error.scala.html" in {
      val helper = newCompilerHelper
      the[CompilationError] thrownBy helper.compile[(() => Html)]("error.scala.html", "html.error") must have(
        Symbol("line") (2),
        Symbol("column") (12)
      )
    }

    "compile templates that have contiguous strings > than 64k" in {
      val helper = newCompilerHelper
      val input = TwirlIO.readFileAsString(new File(sourceDir, "long.scala.html"))
      val result = helper.compile[(() => Html)]("long.scala.html", "html.long").static().toString
      result.length mustBe input.length
      result mustBe input
    }

    "allow rendering a template twice" in {
      val helper = newCompilerHelper
      val inner = helper.compile[((String, List[String]) => (Int) => Html)]("htmlInner.scala.html", "html.htmlInner").static("World", List("A", "B"))(4)

      val outer = helper.compile[Html => Html]("htmlParam.scala.html", "html.htmlParam").static

      outer(inner).body must include("Hello World")
      outer(inner).body must include("Hello World")
    }

    "support injectable templates" when {

      "plain injected template" in {
        val helper = newCompilerHelper
        val template = helper.compile[String => Html]("inject.scala.html", "html.inject").inject("Hello", 10)
        template("world").body.trim mustBe "Hello 10 world"
      }

      "with no args" in {
        val helper = newCompilerHelper
        val template = helper.compile[String => Html]("injectNoArgs.scala.html", "html.injectNoArgs").inject()
        template("Hello").body.trim mustBe "Hello"
      }

      "with parameter groups" in {
        val helper = newCompilerHelper
        val template = helper.compile[String => Html]("injectParamGroups.scala.html", "html.injectParamGroups").inject("Hello", 10, "my")
        template("world").body.trim mustBe "Hello 10 my world"
      }

      "with comments" in {
        val helper = newCompilerHelper
        val template = helper.compile[String => Html]("injectComments.scala.html", "html.injectComments").inject("Hello", 10)
        template("world").body.trim mustBe "Hello 10 world"
      }


    }
  }

  "StringGrouper" should {
    val beer = "\uD83C\uDF7A"
    val line = "abcde" + beer + "fg"

    "split before a surrogate pair" in {
      StringGrouper(line, 5) must contain allOf ("abcde", beer + "fg")
    }

    "not split a surrogate pair" in {
      StringGrouper(line, 6) must contain allOf("abcde" + beer, "fg")
    }

    "split after a surrogate pair" in {
      StringGrouper(line, 7) must contain allOf("abcde" + beer, "fg")
    }
  }

  "compile successfully (elseIf)" when {
    "input is in if clause" in {
      val helper = newCompilerHelper
      val hello = helper.compile[((Int) => Html)]("elseIf.scala.html", "html.elseIf").static(0).toString.trim
      hello must be("hello")
    }
    "input is in else if clause" in {
      val helper = newCompilerHelper
      val hello = helper.compile[((Int) => Html)]("elseIf.scala.html", "html.elseIf").static(1).toString.trim
      hello must be("world")
    }
    "input is in else clause" in {
      val helper = newCompilerHelper
      val hello = helper.compile[((Int) => Html)]("elseIf.scala.html", "html.elseIf").static(25).toString.trim
      hello must be("fail!")
    }
  }

  "compile successfully (if without brackets)" in {
    val helper = newCompilerHelper
    val hello = helper.compile[((String, String) => Html)]("ifWithoutBrackets.scala.html", "html.ifWithoutBrackets")
    hello.static("twirl", "play").toString.trim must be("twirl-play")
    hello.static("twirl", "something-else").toString.trim must be("twirl")
  }

  "compile successfully (complex if without brackets)" in {
    val helper = newCompilerHelper
    val hello = helper.compile[((String, String) => Html)]("ifWithoutBracketsComplex.scala.html", "html.ifWithoutBracketsComplex")
    hello.static("twirl", "play").toString.trim must include("""<header class="play-twirl">""")
    hello.static("twirl", "something-else").toString.trim must include("""<header class="twirl">""")
  }

  "compile successfully (block with tuple)" in {
    val helper = newCompilerHelper
    val hello = helper.compile[(Seq[(String, String)] => Html)]("blockWithTuple.scala.html", "html.blockWithTuple")

    val args = Seq[(String, String)](
      "the-key" -> "the-value"
    )
    hello.static(args).toString.trim must be("the-key => the-value")
  }

  "compile successfully (block with nested tuples)" in {
    val helper = newCompilerHelper
    val hello = helper.compile[(Seq[(String, String)] => Html)]("blockWithNestedTuple.scala.html", "html.blockWithNestedTuple")

    val args = Seq[(String, String)](
      "the-key" -> "the-value"
    )
    hello.static(args).toString.trim must be("the-key => the-value")
  }

}

object Helper {

  case class CompilationError(message: String, line: Int, column: Int) extends RuntimeException(message)

  class CompilerHelper(sourceDir: File, generatedDir: File, generatedClasses: File) {

    import java.net._

    import scala.collection.mutable
    import scala.reflect.internal.util.Position
    import scala.tools.nsc.reporters.ConsoleReporter
    import scala.tools.nsc.{ Global, Settings }

    val twirlCompiler = TwirlCompiler

    val classloader = new URLClassLoader(Array(generatedClasses.toURI.toURL), Class.forName("play.twirl.compiler.TwirlCompiler").getClassLoader)

    // A list of the compile errors from the most recent compiler run
    val compileErrors = new mutable.ListBuffer[CompilationError]

    val compiler = {

      def additionalClassPathEntry: Option[String] = Some(
        Class.forName("play.twirl.compiler.TwirlCompiler").getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(url => new File(url.toURI)).mkString(":"))

      val settings = new Settings
      val scalaObjectSource = Class.forName("scala.Option").getProtectionDomain.getCodeSource

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
        override def display(pos: Position, msg: String, severity: Severity): Unit = {
          pos match {
            case scala.reflect.internal.util.NoPosition => // do nothing
            case _ => compileErrors.append(CompilationError(msg, pos.line, pos.point))
          }
        }
      })

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
          case other => throw new IllegalStateException(className + " does not declare exactly one constructor: " + other)
        }
      }
    }

    def compile[T](templateName: String, className: String, additionalImports: Seq[String] = Nil): CompiledTemplate[T] = {
      val templateFile = new File(sourceDir, templateName)
      val Some(generated) = twirlCompiler.compile(templateFile, sourceDir, generatedDir, "play.twirl.api.HtmlFormat",
        additionalImports = TwirlCompiler.DefaultImports ++ additionalImports)

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
