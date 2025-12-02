/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.compiler
package test

import java.io._
import play.twirl.api.Html
import play.twirl.parser.TwirlIO
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import TwirlCompiler.ScalaCompat
import scala.io.Source

class CompilerSpec extends AnyWordSpec with Matchers {

  import Helper._

  val testName = "Twirl compiler"

  val sourceDir = new File("compiler/src/test/resources")

  def newCompilerHelper = {
    val dirName          = "twirl-parser"
    val generatedDir     = new File("compiler/target/test/" + dirName + "/generated-templates")
    val generatedClasses = new File("compiler/target/test/" + dirName + "/generated-classes")
    TwirlIO.deleteRecursively(generatedDir)
    TwirlIO.deleteRecursively(generatedClasses)
    generatedClasses.mkdirs()
    new CompilerHelper(sourceDir, generatedDir, generatedClasses)
  }

  testName should {
    "compile successfully (real)" in {
      val helper = newCompilerHelper
      val tmpl   = helper
        .compile[((String, List[String]) => (Int) => Html)]("real.scala.html", "html.real")
        .static("World", List("A", "B"))(4)
        .toString
        .trim

      tmpl must (include("<h1>Hello World</h1>").and(include("You have 2 items")).and(include("EA")).and(include("EB")))
    }

    "compile successfully (static)" in {
      val helper = newCompilerHelper
      helper.compile[(() => Html)]("static.scala.html", "html.static").static().toString.trim must be(
        "<h1>It works</h1>"
      )
    }

    "compile successfully (patternMatching)" in {
      val testParam = "12345"
      val helper    = newCompilerHelper
      helper
        .compile[((String) => Html)]("patternMatching.scala.html", "html.patternMatching")
        .static(testParam)
        .toString
        .trim must be("""@test
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
      val hello  = helper.compile[((String) => Html)]("hello.scala.html", "html.hello").static("World").toString.trim
      hello must be("<h1>Hello World!</h1><h1>xml</h1>")
    }

    "compile successfully (helloNull)" in {
      val helper = newCompilerHelper
      val hello  =
        helper.compile[((String) => Html)]("helloNull.scala.html", "html.helloNull").static(null).toString.trim
      hello must be("<h1>Hello !</h1>")
    }

    "compile successfully (zipWithIndex)" in {
      val helper = newCompilerHelper
      val output = helper
        .compile[((Seq[String]) => Html)]("zipWithIndex.scala.html", "html.zipWithIndex")
        .static(Seq("Alice", "Bob", "Charlie"))
        .toString
        .trim
        .replace("\n", "")
      output must be("<h1>0 Hello Alice!</h1><h1>1 Hello Bob!</h1><h1>2 Hello Charlie!</h1>")
    }

    "compile successfully (set)" in {
      val helper = newCompilerHelper
      val set    = helper
        .compile[((collection.immutable.Set[String]) => Html)]("set.scala.html", "html.set")
        .static(Set("first", "second", "third"))
        .toString
        .trim
        .replace("\n", "")
        .replaceAll("\\s+", "")
      set must be("firstsecondthird")
    }

    "compile successfully (arg imports)" in {
      val helper = newCompilerHelper
      val result = helper
        .compile[((java.io.File, java.net.URL) => Html)]("argImports.scala.html", "html.argImports")
        .static(new java.io.File("example"), new java.net.URI("http://example.org").toURL)
        .toString
        .trim
      result must be("<p>file: example, url: http://example.org</p>")
    }

    "compile successfully (default imports)" in {
      val helper = newCompilerHelper
      val result =
        helper.compile[(() => Html)]("importsDefault.scala.html", "html.importsDefault").static().toString.trim
      result must be("foo")
    }

    "compile successfully (escape closing brace)" in {
      val helper = newCompilerHelper
      val result = helper
        .compile[(Option[String] => Html)]("escapebrace.scala.html", "html.escapebrace")
        .static(Some("foo"))
        .toString
        .trim
      result must be("foo: }")
    }

    "compile successfully (utf8)" in {
      val helper = newCompilerHelper
      val text   = helper.compile[(() => Html)]("utf8.scala.html", "html.utf8").static().toString.trim
      text must be("€, ö, or ü")
    }

    "compile successfully (existential)" in {
      val helper = newCompilerHelper
      val text   = helper
        .compile[(List[?] => Html)]("existential.scala.html", "html.existential")
        .static(List(1, 2, 3))
        .toString
        .trim
      text must be("123")
    }

    "compile successfully (triple quotes)" in {
      val helper = newCompilerHelper
      val out    = helper.compile[(() => Html)]("triplequotes.scala.html", "html.triplequotes").static().toString.trim
      out must be("\"\"\"\n\n\"\"\"\"\"\n\n\"\"\"\"\"\"")
    }

    "compile successfully (var args existential)" in {
      val helper = newCompilerHelper
      val text   = helper
        .compile[(Array[List[?]] => Html)]("varArgsExistential.scala.html", "html.varArgsExistential")
        .static(Array(List(1, 2, 3), List(4, 5, 6)))
        .toString
        .trim

      val compat        = ScalaCompat(Option(BuildInfo.scalaVersion))
      val generatedFile = helper.generatedDir.toPath.resolve("html/varArgsExistential.template.scala").toFile
      val generatedText = Source.fromFile(generatedFile).getLines().mkString("\n")

      generatedText must include(s"list.toIndexedSeq${compat.varargSplicesSyntax}")
      text must be("123456")
    }

    "compile successfully (using)" in {

      assume(BuildInfo.scalaVersion.startsWith("3."), "This test is only supposed to run with scala3")
      val helper = newCompilerHelper

      val text = helper
        .compile[(String => String => Html)]("using.scala.html", "html.using")
        .static("the using modifier")("should compile 😤")
        .toString
        .trim

      val compat        = ScalaCompat(Option(BuildInfo.scalaVersion))
      val generatedFile = helper.generatedDir.toPath.resolve("html/using.template.scala").toFile
      val generatedText = Source.fromFile(generatedFile).getLines().mkString("\n")

      generatedText must include(s"apply(x)(${compat.usingSyntax}y)")
      text must be("the using modifier should compile 😤")
    }

    "compile successfully (call by name)" in {
      val helper = newCompilerHelper
      val text   = helper
        .compile[((=> String) => Html)]("callByName.scala.html", "html.callByName")
        .static("World")
        .toString
        .trim
      text must be("<h1>Hello World!</h1>")
    }

    "compile successfully (if/else/elseIf)" when {
      "input is in if clause" in {
        val helper = newCompilerHelper
        val hello  = helper.compile[((Int) => Html)]("elseIf.scala.html", "html.elseIf").static(0).toString.trim
        hello must be("hello")
      }
      "input is in else if clause" in {
        val helper = newCompilerHelper
        val hello  = helper.compile[((Int) => Html)]("elseIf.scala.html", "html.elseIf").static(1).toString.trim
        hello must be("world")
      }
      "input is in else clause" in {
        val helper = newCompilerHelper
        val hello  = helper.compile[((Int) => Html)]("elseIf.scala.html", "html.elseIf").static(25).toString.trim
        hello must be("fail!")
      }

      "if with brackets" in {
        val helper = newCompilerHelper
        val hello  = helper.compile[(String => Html)]("ifWithBrackets.scala.html", "html.ifWithBrackets")
        hello.static("twirl").toString.trim must be("twirl")
        hello.static("something-else").toString.trim must be("")
      }

      "if without brackets" in {
        val helper = newCompilerHelper
        val hello = helper.compile[((String, String) => Html)]("ifWithoutBrackets.scala.html", "html.ifWithoutBrackets")
        hello.static("twirl", "play").toString.trim must be("twirl-play")
        hello.static("twirl", "something-else").toString.trim must be("twirl")
      }

      "complex if without brackets" in {
        val helper = newCompilerHelper
        val hello  =
          helper
            .compile[((String, String) => Html)]("ifWithoutBracketsComplex.scala.html", "html.ifWithoutBracketsComplex")
        hello.static("twirl", "play").toString.trim must include("""<header class="play-twirl">""")
        hello.static("twirl", "something-else").toString.trim must include("""<header class="twirl">""")
      }

      "'if'/'else if'/'else' containing imports, val, def and subtemplates" in {
        val helper = newCompilerHelper
        val hello  =
          helper.compile[((String, String, Integer, Integer) => Html)](
            "ifimportsValDefSubtemplates.scala.html",
            "html.ifimportsValDefSubtemplates"
          )
        hello.static("twirl", "play", 1, 1).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """value1def1text1scalablock1<div><headerclass="play-twirl1">twirl/0subdef1value1_sub1</header></div>shadow_value1value2def2text2scalablock2<div><headerclass="play-twirl2">twirl/0subdef2value2_sub2</header></div>alwaysvisible"""
        )
        hello.static("twirl", "play", 1, 2).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """value1def1text1scalablock1<div><headerclass="play-twirl1">twirl/0subdef1value1_sub1</header></div>value3def3text3scalablock3<div><headerclass="play-twirl3">twirl/0subdef3value3_sub3</header></div>alwaysvisible"""
        )
        hello.static("twirl", "play", 1, 3).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """value1def1text1scalablock1<div><headerclass="play-twirl1">twirl/0subdef1value1_sub1</header></div>value4def4text4scalablock4<div><headerclass="play-twirl4">twirl/0subdef4value4_sub4</header></div>alwaysvisible"""
        )

        hello.static("twirl", "play", 2, 1).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """elseif_value1elseif_def1text1elseif_scalablock1<div><headerclass="elseif_play-twirl1">twirl/0subdef1elseif_value1_sub1</header></div>elseif_shadow_value1elseif_value2elseif_def2text2elseif_scalablock2<div><headerclass="elseif_play-twirl2">twirl/0subdef2elseif_value2_sub2</header></div>alwaysvisible"""
        )
        hello.static("twirl", "play", 2, 2).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """elseif_value1elseif_def1text1elseif_scalablock1<div><headerclass="elseif_play-twirl1">twirl/0subdef1elseif_value1_sub1</header></div>elseif_value3elseif_def3text3elseif_scalablock3<div><headerclass="elseif_play-twirl3">twirl/0subdef3elseif_value3_sub3</header></div>alwaysvisible"""
        )
        hello.static("twirl", "play", 2, 3).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """elseif_value1elseif_def1text1elseif_scalablock1<div><headerclass="elseif_play-twirl1">twirl/0subdef1elseif_value1_sub1</header></div>elseif_value4elseif_def4text4elseif_scalablock4<div><headerclass="elseif_play-twirl4">twirl/0subdef4elseif_value4_sub4</header></div>alwaysvisible"""
        )

        hello.static("twirl", "play", 3, 1).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """else_value1else_def1text1else_scalablock1<div><headerclass="else_play-twirl1">twirl/0subdef1else_value1_sub1</header></div>else_shadow_value1else_value2else_def2text2else_scalablock2<div><headerclass="else_play-twirl2">twirl/0subdef2else_value2_sub2</header></div>alwaysvisible"""
        )
        hello.static("twirl", "play", 3, 2).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """else_value1else_def1text1else_scalablock1<div><headerclass="else_play-twirl1">twirl/0subdef1else_value1_sub1</header></div>else_value3else_def3text3else_scalablock3<div><headerclass="else_play-twirl3">twirl/0subdef3else_value3_sub3</header></div>alwaysvisible"""
        )
        hello.static("twirl", "play", 3, 3).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """else_value1else_def1text1else_scalablock1<div><headerclass="else_play-twirl1">twirl/0subdef1else_value1_sub1</header></div>else_value4else_def4text4else_scalablock4<div><headerclass="else_play-twirl4">twirl/0subdef4else_value4_sub4</header></div>alwaysvisible"""
        )

        hello.static("twirl", "something-else", 1, 1).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """value1def1text1scalablock1<div><headerclass="twirl1">twirl/0subdef1value1_sub1</header></div>shadow_value1value2def2text2scalablock2<div><headerclass="twirl2">twirl/0subdef2value2_sub2</header></div>alwaysvisible"""
        )
        hello.static("twirl", "something-else", 1, 2).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """value1def1text1scalablock1<div><headerclass="twirl1">twirl/0subdef1value1_sub1</header></div>value3def3text3scalablock3<div><headerclass="twirl3">twirl/0subdef3value3_sub3</header></div>alwaysvisible"""
        )
        hello.static("twirl", "something-else", 1, 3).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """value1def1text1scalablock1<div><headerclass="twirl1">twirl/0subdef1value1_sub1</header></div>value4def4text4scalablock4<div><headerclass="twirl4">twirl/0subdef4value4_sub4</header></div>alwaysvisible"""
        )

        hello.static("twirl", "something-else", 2, 1).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """elseif_value1elseif_def1text1elseif_scalablock1<div><headerclass="elseif_twirl1">twirl/0subdef1elseif_value1_sub1</header></div>elseif_shadow_value1elseif_value2elseif_def2text2elseif_scalablock2<div><headerclass="elseif_twirl2">twirl/0subdef2elseif_value2_sub2</header></div>alwaysvisible"""
        )
        hello.static("twirl", "something-else", 2, 2).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """elseif_value1elseif_def1text1elseif_scalablock1<div><headerclass="elseif_twirl1">twirl/0subdef1elseif_value1_sub1</header></div>elseif_value3elseif_def3text3elseif_scalablock3<div><headerclass="elseif_twirl3">twirl/0subdef3elseif_value3_sub3</header></div>alwaysvisible"""
        )
        hello.static("twirl", "something-else", 2, 3).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """elseif_value1elseif_def1text1elseif_scalablock1<div><headerclass="elseif_twirl1">twirl/0subdef1elseif_value1_sub1</header></div>elseif_value4elseif_def4text4elseif_scalablock4<div><headerclass="elseif_twirl4">twirl/0subdef4elseif_value4_sub4</header></div>alwaysvisible"""
        )

        hello.static("twirl", "something-else", 3, 1).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """else_value1else_def1text1else_scalablock1<div><headerclass="else_twirl1">twirl/0subdef1else_value1_sub1</header></div>else_shadow_value1else_value2else_def2text2else_scalablock2<div><headerclass="else_twirl2">twirl/0subdef2else_value2_sub2</header></div>alwaysvisible"""
        )
        hello.static("twirl", "something-else", 3, 2).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """else_value1else_def1text1else_scalablock1<div><headerclass="else_twirl1">twirl/0subdef1else_value1_sub1</header></div>else_value3else_def3text3else_scalablock3<div><headerclass="else_twirl3">twirl/0subdef3else_value3_sub3</header></div>alwaysvisible"""
        )
        hello.static("twirl", "something-else", 3, 3).toString.trim.replace("\n", "").replaceAll("\\s+", "") must be(
          """else_value1else_def1text1else_scalablock1<div><headerclass="else_twirl1">twirl/0subdef1else_value1_sub1</header></div>else_value4else_def4text4else_scalablock4<div><headerclass="else_twirl4">twirl/0subdef4else_value4_sub4</header></div>alwaysvisible"""
        )
      }
    }

    "compile successfully (local definitions)" in {
      val helper = newCompilerHelper
      val hello  =
        helper.compile[(() => Html)]("localDef.scala.html", "html.localDef")
      hello.static().toString.trim must be(
        "Play-Framework-Vienna-Austria-Europe-The High Velocity Web Framework For Java and Scala-2023"
      )
    }

    "compile successfully (block with tuple)" in {
      val helper = newCompilerHelper
      val hello  = helper.compile[(Seq[(String, String)] => Html)]("blockWithTuple.scala.html", "html.blockWithTuple")

      val args = Seq[(String, String)](
        "the-key" -> "the-value"
      )
      hello.static(args).toString.trim must be("the-key => the-value")
    }

    "compile successfully (block with nested tuples)" in {
      val helper = newCompilerHelper
      val hello  =
        helper.compile[(Seq[(String, String)] => Html)]("blockWithNestedTuple.scala.html", "html.blockWithNestedTuple")

      val args = Seq[(String, String)](
        "the-key" -> "the-value"
      )
      hello.static(args).toString.trim must be("the-key => the-value")
    }

    "compile successfully (nested templates)" in {
      val helper = newCompilerHelper
      val hello  = helper.compile[(() => Html)]("nestedTemplates.scala.html", "html.nestedTemplates")
      hello.static().toString.trim.replaceAll(" ", "").replaceAll("\n", "") must be(
        """_same_random_in_val__same_random_in_lazy_val__first__defstr__tmpl-defstr__defstr_goodtext_in_between_inner_inner_tmpl_
          |__tmplinner_defstr___samename1__samedefname1__samename2__samedefname2_
          |_same_random_in_val__same_random_in_lazy_val__second__defstr__tmpl-defstr__defstr_goodtext_in_between_inner_inner_tmpl_
          |__tmplinner_defstr___samename1__samedefname1__samename2__samedefname2_""".stripMargin.replaceAll("\n", "")
      )
    }

    "compile successfully ([lazy] val)" in {
      val helper = newCompilerHelper
      val hello  = helper.compile[(() => Html)]("vals.scala.html", "html.vals")
      hello.static().toString.trim.replaceAll(" ", "").replaceAll("\n", "") must be(
        """step1:0_step2:0_step3:1_step4:5_step5:0_step6:0_step7:0_step8:0
          |_step9:0_step10:4_step11:4_step12:5
          |""".stripMargin.replaceAll("\n", "")
      )
    }

    "keep order of member and template blocks, as defined in the source template" in {
      val helper = newCompilerHelper
      val hello  = helper.compile[(() => Html)]("codeBlockOrder.scala.html", "html.codeBlockOrder")
      hello.static().toString.trim.replaceAll(" ", "").replaceAll("\n", "") must be("123456789101112")
    }

    "fail compilation for error.scala.html" in {
      val helper = newCompilerHelper
      the[CompilationError] thrownBy helper.compile[(() => Html)]("error.scala.html", "html.error") must have(
        Symbol("line")(5),
        Symbol("point")(463)
      )
    }

    "fail compilation for errorInTemplateArgs.scala.html" in {
      val helper = newCompilerHelper
      the[CompilationError] thrownBy helper
        .compile[(() => Html)]("errorInTemplateArgs.scala.html", "html.errorInTemplateArgs") must have(
        Symbol("line")(5),
        Symbol("point")(458)
      )
    }

    "compile templates that have contiguous strings > than 64k" in {
      val helper = newCompilerHelper
      val input  = TwirlIO
        .readFileAsString(new File(sourceDir, "long.scala.html"))
        .replaceAll("(?s)@\\*(.*)\\*@(\n)*", "") // drop block comments
      val result = helper.compile[(() => Html)]("long.scala.html", "html.long").static().toString
      result.length mustBe input.length
      result mustBe input
    }

    "allow rendering a template twice" in {
      val helper = newCompilerHelper
      val inner  = helper
        .compile[((String, List[String]) => (Int) => Html)]("htmlInner.scala.html", "html.htmlInner")
        .static("World", List("A", "B"))(4)

      val outer = helper.compile[Html => Html]("htmlParam.scala.html", "html.htmlParam").static

      outer(inner).body must include("Hello World")
      outer(inner).body must include("Hello World")
    }

    "support injectable templates" when {
      "plain injected template" in {
        val helper   = newCompilerHelper
        val template = helper.compile[String => Html]("inject.scala.html", "html.inject").inject("Hello", 10)
        template("world").body.trim mustBe "Hello 10 world"
      }

      "with no args" in {
        val helper   = newCompilerHelper
        val template = helper.compile[String => Html]("injectNoArgs.scala.html", "html.injectNoArgs").inject()
        template("Hello").body.trim mustBe "Hello"
      }

      "with parameter groups" in {
        val helper   = newCompilerHelper
        val template = helper
          .compile[String => Html]("injectParamGroups.scala.html", "html.injectParamGroups")
          .inject("Hello", 10, "my")
        template("world").body.trim mustBe "Hello 10 my world"
      }

      "with comments" in {
        val helper   = newCompilerHelper
        val template =
          helper.compile[String => Html]("injectComments.scala.html", "html.injectComments").inject("Hello", 10)
        template("world").body.trim mustBe "Hello 10 world"
      }
    }
  }

  "StringGrouper" should {
    val beer = "\uD83C\uDF7A"
    val line = "abcde" + beer + "fg"

    "split before a surrogate pair" in {
      (StringGrouper(line, 5) must contain).allOf("abcde", beer + "fg")
    }

    "not split a surrogate pair" in {
      (StringGrouper(line, 6) must contain).allOf("abcde" + beer, "fg")
    }

    "split after a surrogate pair" in {
      (StringGrouper(line, 7) must contain).allOf("abcde" + beer, "fg")
    }
  }

  "ScalaCompat" should {

    val cases = List(
      None            -> ": _*",
      Some("2.12.18") -> ": _*",
      Some("2.13.12") -> ": _*",
      Some("3.3.1")   -> "*"
    )

    "produce correct varargs splice syntax" in {

      cases.foreach { case (version, expected) =>
        ScalaCompat(version).varargSplicesSyntax must be(expected)
      }

    }

  }

}
