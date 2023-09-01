/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import static java.nio.charset.StandardCharsets.UTF_8;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

abstract class AbstractFunctionalTest {

  @TempDir File projectDir;

  File projectSourceDir;

  GradleRunner runner;

  Configuration freemarkerConf;

  protected abstract File getProjectSourceDir();

  protected abstract String getBuildFileContent();

  protected String getSettingsFileContent() {
    return "";
  }

  static String getScalaVersion() {
    return System.getProperty("scala.version", TwirlPlugin.DEFAULT_SCALA_VERSION);
  }

  static String getTwirlVersion() {
    return System.getProperty("twirl.version");
  }

  protected Path projectSourcePath(String path) {
    return Paths.get(projectSourceDir.getAbsolutePath(), path);
  }

  protected Path projectPath(String path) {
    return Paths.get(projectDir.getAbsolutePath(), path);
  }

  protected Path projectBuildPath(String path) {
    return Paths.get(projectDir.getAbsolutePath(), "build/" + path);
  }

  @BeforeEach
  void init() throws IOException, TemplateException {
    projectSourceDir = getProjectSourceDir();
    runner = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().forwardOutput();

    initFreemarker();

    FileUtils.writeStringToFile(
        projectPath("build.gradle.kts").toFile(), getBuildFileContent(), UTF_8);
    FileUtils.writeStringToFile(
        projectPath("settings.gradle.kts").toFile(), getSettingsFileContent(), UTF_8);
  }

  protected void initFreemarker() throws IOException {
    freemarkerConf = new Configuration(Configuration.VERSION_2_3_32);
    freemarkerConf.setDirectoryForTemplateLoading(projectSourceDir);
  }

  protected String templateProcess(String template, Map<String, Object> params) {
    StringWriter writer = new StringWriter();
    try {
      Template buildGradle = freemarkerConf.getTemplate(template);
      buildGradle.process(params, writer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  protected BuildResult build(String... args) {
    return runner.withArguments(args).build();
  }
}
