/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "compile", defaultPhase = GENERATE_SOURCES, threadSafe = true)
public class TwirlCompileMojo extends AbstractTwirlCompileMojo {

  /**
   * The directories in which the Twirl templates is found.
   *
   * <p>Default: ${project.basedir}/src/main/twirl
   *
   * <p>Example:
   *
   * <pre>{@code
   * <sourceDir>${project.basedir}/src/main/templates</sourceDir>
   * }</pre>
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/twirl")
  private File sourceDir;

  /**
   * The directory where the compiled Twirl templates are placed.
   *
   * <p>Default: {@code ${project.build.directory}/generated-sources/twirl}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <outputDir>${project.build.directory}/generated-sources/twirl</outputDir>
   * }</pre>
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/twirl")
  private File outputDir;

  @Override
  protected File getSourceDirectory() {
    return sourceDir;
  }

  @Override
  protected File getOutputDirectory() {
    return outputDir;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    super.execute();
    project.addCompileSourceRoot(getOutputDirectory().getAbsolutePath());
    getLog().info("Added generated Scala sources: " + getOutputDirectory());
  }
}
