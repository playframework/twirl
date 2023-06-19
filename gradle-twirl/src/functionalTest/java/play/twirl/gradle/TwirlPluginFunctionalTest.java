/*
* Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
*/
package play.twirl.gradle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A simple functional test for the 'com.playframework.twirl' plugin. */
class TwirlPluginFunctionalTest {
  @TempDir File projectDir;

  private File getBuildFile() {
    return new File(projectDir, "build.gradle");
  }

  private File getSettingsFile() {
    return new File(projectDir, "settings.gradle");
  }

  @Test
  void canRunTask() throws IOException {
    writeString(getSettingsFile(), "");
    writeString(getBuildFile(), "plugins {" + "  id('com.typesafe.play.twirl')" + "}");

    // Run the build
    GradleRunner runner = GradleRunner.create();
    runner.forwardOutput();
    runner.withPluginClasspath();
    runner.withArguments("greeting");
    runner.withProjectDir(projectDir);
    BuildResult result = runner.build();

    // Verify the result
    assertTrue(result.getOutput().contains("Hello from plugin 'com.playframework.twirl'"));
  }

  private void writeString(File file, String string) throws IOException {
    try (Writer writer = new FileWriter(file)) {
      writer.write(string);
    }
  }
}
