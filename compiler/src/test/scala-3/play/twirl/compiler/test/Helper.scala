/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.compiler
package test

import java.io.File

class Helper(sourceDir: File, generatedDir: File, generatedClasses: File) {
  def compile[T](
      templateName: String,
      className: String,
      additionalImports: Seq[String] = Nil
  ): CompiledTemplate[T] = ???
}
