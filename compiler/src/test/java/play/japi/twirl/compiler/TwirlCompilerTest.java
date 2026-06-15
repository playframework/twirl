/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.japi.twirl.compiler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class TwirlCompilerTest {

  @Test
  public void compile() {
    File sourceDirectory = new File("compiler/src/test/resources");
    File source = new File(sourceDirectory, "real.scala.html");
    File generatedDirectory = new File("compiler/target/test/japi-compiler/generated-templates");

    deleteRecursively(generatedDirectory);
    generatedDirectory.mkdirs();

    Optional<File> result =
        TwirlCompiler.compile(
            source,
            sourceDirectory,
            generatedDirectory,
            "play.twirl.api.HtmlFormat",
            TwirlCompiler.DEFAULT_IMPORTS,
            new ArrayList<String>());
    assertTrue(result.isPresent());

    Optional<File> recompilationResult =
        TwirlCompiler.compile(
            source,
            sourceDirectory,
            generatedDirectory,
            "play.twirl.api.HtmlFormat",
            TwirlCompiler.DEFAULT_IMPORTS,
            new ArrayList<String>());
    assertFalse(recompilationResult.isPresent());
  }

  @Test
  public void defaultImportsHaveDeterministicOrder() {
    String wildcard =
        play.twirl.compiler.BuildInfo$.MODULE$.scalaVersion().startsWith("3.") ? "*" : "_";
    List<String> expected =
        new ArrayList<>(
            Arrays.asList(
                "_root_.play.twirl.api.TwirlFeatureImports." + wildcard,
                "_root_.play.twirl.api.TwirlHelperImports." + wildcard));
    if ("*".equals(wildcard)) {
      expected.add("scala.language.adhocExtensions");
    }
    expected.addAll(
        Arrays.asList(
            "_root_.play.twirl.api.Html",
            "_root_.play.twirl.api.JavaScript",
            "_root_.play.twirl.api.Txt",
            "_root_.play.twirl.api.Xml"));

    assertEquals(expected, new ArrayList<>(TwirlCompiler.DEFAULT_IMPORTS));
  }

  private static void deleteRecursively(File directory) {
    if (!directory.exists()) {
      return;
    }
    Path path = directory.toPath();
    try {
      Files.walkFileTree(
          path,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
