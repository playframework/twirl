/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static play.twirl.gradle.TwirlPlugin.javaPluginExtension;

import java.nio.charset.StandardCharsets;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration;
import org.gradle.api.tasks.SourceSet;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A simple unit test to check a Twirl Gradle Plugin */
class TwirlPluginTest {

  private Project project;

  @BeforeEach
  void init() {
    project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("application");
    project.getPluginManager().apply("org.playframework.twirl");
  }

  @Test
  @DisplayName("Twirl extension should be registered")
  void extensionShouldBeRegistered() {
    TwirlExtension ext = (TwirlExtension) project.getExtensions().findByName("twirl");
    assertThat(ext).isNotNull();
    assertThat((ext).getScalaVersion().getOrNull()).isEqualTo("2.13");
  }

  @Test
  @DisplayName("Twirl configuration should be registered")
  void configurationShouldBeRegistered() {
    Configuration conf = project.getConfigurations().findByName("twirl");
    assertThat(conf).isNotNull();
    assertThat(conf.isTransitive()).isTrue();
    assertThat(conf.isVisible()).isFalse();
    ((DefaultConfiguration) conf).runDependencyActions();
    assertThat(conf.getDependencies())
        .anyMatch(
            dependency ->
                "org.playframework.twirl".equals(dependency.getGroup())
                    && dependency.getName().startsWith("twirl-compiler"));
  }

  @Test
  @DisplayName("Twirl source directory set should be registered for main source set")
  void sourceDirectorySetShouldBeRegisteredForMainSourceSet() {
    checkSourceDirectorySet(javaPluginExtension(project).getSourceSets().getByName("main"));
  }

  @Test
  @DisplayName("Twirl source directory set should be registered for test source set")
  void sourceDirectorySetShouldBeRegisteredForTestSourceSet() {
    checkSourceDirectorySet(javaPluginExtension(project).getSourceSets().getByName("test"));
  }

  @Test
  @DisplayName("Twirl compile task should be registered for main/test source sets")
  void compileTasksShouldBeRegistered() {
    assertThat(project.getTasks().findByName("compileTwirl")).isNotNull();
    assertThat(project.getTasks().findByName("compileTestTwirl")).isNotNull();
  }

  private void checkSourceDirectorySet(SourceSet sourceSet) {
    assertThat(sourceSet).isNotNull();
    TwirlSourceDirectorySet twirlSourceSet =
        sourceSet.getExtensions().findByType(TwirlSourceDirectorySet.class);
    assertThat(twirlSourceSet).isNotNull();
    assertThat(twirlSourceSet.getName()).isEqualTo("twirl");
    assertThat(twirlSourceSet.getSourceEncoding().getOrNull())
        .isEqualTo(StandardCharsets.UTF_8.name());
    assertThat(twirlSourceSet.getTemplateImports().get()).isEmpty();
    assertThat(twirlSourceSet.getConstructorAnnotations().get()).isEmpty();
    assertThat(twirlSourceSet.getTemplateFormats().get()).containsKeys("html", "txt", "js", "xml");
  }
}
