/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.groovy.util.Maps;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** A simple functional test to check a Twirl Gradle Plugin. */
public class TwirlPluginFunctionalTest extends AbstractFunctionalTest {

  @Override
  protected File getProjectSourceDir() {
    return new File("src/test/resources/simple");
  }

  @Override
  protected String getBuildFileContent() {
    Map<String, Object> params =
        Maps.of(
            "scalaVersion", getScalaVersion(),
            "twirlVersion", getTwirlVersion());
    return templateProcess("build.gradle.kts.ftlh", params);
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test simple Gradle project with Twirl HTML template")
  void testSimpleGradleProject(String gradleVersion) throws IOException {
    File simpleSources = projectPath("src").toFile();
    FileUtils.copyDirectory(projectSourcePath("src").toFile(), simpleSources);

    BuildResult result = build(gradleVersion, "build");

    BuildTask compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/c.template.scala"))
        .isNotEmptyFile()
        .binaryContent()
        .asString()
        .contains("import java.lang._", "class c @java.lang.Deprecated()");

    BuildTask compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath("classes/scala/main/a/b/html/c.class")).isNotEmptyFile();

    result = build(gradleVersion, "build");

    compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
  }
}
