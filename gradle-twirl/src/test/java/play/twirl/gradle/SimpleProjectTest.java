/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
@DisplayName("Simple Gradle project with Twirl HTML template")
public class SimpleProjectTest extends AbstractFunctionalTest {

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

  @Override
  protected String getSettingsFileContent() {
    return templateProcess("settings.gradle.kts.ftlh", Collections.emptyMap());
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test common build")
  void testCommonBuild(String gradleVersion) throws IOException {
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
        .contains("import java.lang._", "class c @java.lang.Deprecated()", "import a.b.html._");

    BuildTask compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath("classes/scala/main/a/b/html/c.class")).isNotEmptyFile();
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test UP-TO-DATE behavior for build without changes in templates")
  void testUpToDateBuild(String gradleVersion) throws IOException {
    File simpleSources = projectPath("src").toFile();
    FileUtils.copyDirectory(projectSourcePath("src").toFile(), simpleSources);

    build(gradleVersion, "build");

    BuildResult result = build(gradleVersion, "build");

    BuildTask compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);

    BuildTask compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test incremental compilation after add and delete template")
  void testIncrementalBuild(String gradleVersion) throws IOException {
    File simpleSources = projectPath("src").toFile();
    FileUtils.copyDirectory(projectSourcePath("src").toFile(), simpleSources);

    build(gradleVersion, "build");

    // Add new Twirl template
    Path newTemplate = projectPath("src/main/twirl/a/b/d.scala.html");
    Files.copy(projectSourcePath("src/main/twirl/a/b/c.scala.html"), newTemplate);

    BuildResult result = build(gradleVersion, "build");

    BuildTask compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/d.template.scala"))
        .isNotEmptyFile();

    BuildTask compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath("classes/scala/main/a/b/html/d.class")).isNotEmptyFile();

    // Delete twirl template
    Files.delete(newTemplate);

    result = build(gradleVersion, "build");

    compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/d.template.scala"))
        .doesNotExist();

    compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath("classes/scala/main/a/b/html/d.class")).doesNotExist();
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test build cache")
  void testBuildCache(String gradleVersion) throws IOException {
    File simpleSources = projectPath("src").toFile();
    FileUtils.copyDirectory(projectSourcePath("src").toFile(), simpleSources);
    Path newTemplate = projectPath("src/main/twirl/a/b/d.scala.html");
    Files.copy(projectSourcePath("src/main/twirl/a/b/c.scala.html"), newTemplate);

    BuildResult result = build(gradleVersion, "--build-cache", "compileTwirl");

    BuildTask compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/c.template.scala"))
        .isNotEmptyFile();
    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/d.template.scala"))
        .isNotEmptyFile();

    build(gradleVersion, "clean");

    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/c.template.scala"))
        .doesNotExist();
    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/d.template.scala"))
        .doesNotExist();

    result = build(gradleVersion, "--build-cache", "compileTwirl");

    compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.FROM_CACHE);

    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/c.template.scala"))
        .isNotEmptyFile();
    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/d.template.scala"))
        .isNotEmptyFile();

    build(gradleVersion, "clean");

    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/c.template.scala"))
        .doesNotExist();
    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/d.template.scala"))
        .doesNotExist();

    Files.delete(newTemplate);

    result = build(gradleVersion, "--build-cache", "compileTwirl");

    compileTwirlResult = result.task(":compileTwirl");
    assertThat(compileTwirlResult).isNotNull();
    assertThat(compileTwirlResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/c.template.scala"))
        .isNotEmptyFile();
    assertThat(projectBuildPath("generated/sources/twirl/main/a/b/html/d.template.scala"))
        .doesNotExist();
  }
}
