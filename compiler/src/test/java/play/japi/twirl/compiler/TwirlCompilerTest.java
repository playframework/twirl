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
