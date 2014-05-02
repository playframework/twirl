package play.twirl.compiler.test

import java.io.File
import play.twirl.api.Html
import play.twirl.compiler.test.Helper.CompilerHelper
import play.twirl.parser.TwirlIO

/**
 * Easiest way to run this:
 *
 * sbt compiler/test:runMain play.twirl.compiler.test.Benchmark
 */
object Benchmark extends App {

  val sourceDir = new File("src/test/resources")
  val generatedDir = new File("target/test/benchmark-templates")
  val generatedClasses = new File("target/test/benchmark-classes")


  TwirlIO.deleteRecursively(generatedDir)
  TwirlIO.deleteRecursively(generatedClasses)
  generatedClasses.mkdirs()

  val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)

  println("Compiling template...")
  val template = helper.compile[((String, List[String]) => (Int) => Html)]("real.scala.html", "html.real")
  val input = (1 to 100).map(_.toString).toList

  // warmup
  println("Warming up...")
  for (i <- 1 to 10000) {
    template("World", input)(4).body
  }

  println("Starting first run...")
  val start1 = System.currentTimeMillis()
  for (i <- 1 to 100000) {
    template("World", input)(4).body
  }
  println("First run: " + (System.currentTimeMillis() - start1) + "ms")

  println("Starting second run...")
  val start2 = System.currentTimeMillis()
  for (i <- 1 to 100000) {
    template("World", input)(4).body
  }
  println("Second run: " + (System.currentTimeMillis() - start2) + "ms")

}
