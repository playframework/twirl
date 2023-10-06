/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.list;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Simple Maven project with Twirl HTML template")
public class SimpleProjectTest {

  @Test
  @DisplayName("Test common build")
  public void testCommonBuild() {
    var compiledTemplates =
        list(
            projectBuildPath("generated-sources/twirl/a/b/html/c.template.scala"),
            projectBuildPath("generated-sources/twirl/a/b/html/f.template.scala"),
            projectBuildPath("generated-test-sources/twirl/a/b/html/d.template.scala"));

    assertThat(compiledTemplates)
        .allSatisfy(
            file ->
                assertThat(file)
                    .isNotEmptyFile()
                    .binaryContent()
                    .asString()
                    .contains("import java.lang._", "@java.lang.Deprecated()"));

    var compiledScalaSources =
        list(
            projectBuildPath("classes/a/b/html/c.class"),
            projectBuildPath("classes/a/b/html/f.class"),
            projectBuildPath("test-classes/a/b/html/d.class"));

    assertThat(compiledScalaSources).allSatisfy(file -> assertThat(file).isNotEmptyFile());
  }

  protected Path projectBuildPath(String path) {
    return Paths.get("target", path);
  }
}
