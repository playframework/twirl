/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.maven;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.codehaus.plexus.util.FileUtils.getExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import play.japi.twirl.compiler.TwirlCompiler;
import scala.io.Codec;

public abstract class AbstractTwirlCompileMojo extends AbstractMojo {

  private static final Map<String, String> DEFAULT_TEMPLATE_FORMATS =
      Map.of(
          "html",
          "play.twirl.api.HtmlFormat",
          "txt",
          "play.twirl.api.TxtFormat",
          "xml",
          "play.twirl.api.XmlFormat",
          "js",
          "play.twirl.api.JavaScriptFormat");

  /** The maven project. */
  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;

  protected abstract File getSourceDirectory();

  protected abstract File getOutputDirectory();

  /**
   * A set of inclusion filters for the Twirl compiler.
   *
   * <p>Default: {@code **}{@code /*.scala.*}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <includes>
   *   <include>}{@code **}{@code /Include.scala.html</include>
   * </includes>
   * }</pre>
   */
  @Parameter protected Set<String> includes = new TreeSet<>();

  /**
   * A set of exclusion filters for the Twirl compiler.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <excludes>
   *   <exclude>{@code **}{@code *}/Exclude.scala.html</exclude>
   * </excludes>
   * }</pre>
   */
  @Parameter private Set<String> excludes = new TreeSet<>();

  /**
   * Defined twirl template formats.
   *
   * <p>Default: {@link #DEFAULT_TEMPLATE_FORMATS}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <templateFormats>
   *   <html>play.twirl.api.HtmlFormat</html>
   * </templateFormats>
   * }</pre>
   */
  @Parameter(name = "templateFormats")
  private Map<String, String> templateFormats = new LinkedHashMap<>();

  /**
   * Imports that should be added to generated source files.
   *
   * <p>Default: {@link play.japi.twirl.compiler.TwirlCompiler#DEFAULT_IMPORTS}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <templateImports>
   *   <import>org.abc.backend._</import>
   * </templateImports>
   * }</pre>
   */
  @Parameter(name = "templateImports")
  private Set<String> templateImports = new LinkedHashSet<>();

  /**
   * Annotations added to constructors in injectable templates.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <constructorAnnotations>
   *   <annotation>@javax.inject.Inject()</annotation>
   * </constructorAnnotations>
   * }</pre>
   */
  @Parameter(name = "constructorAnnotations")
  private Set<String> constructorAnnotations = new LinkedHashSet<>();

  /**
   * Source encoding for template files and generated scala files.
   *
   * <p>Default: {@code UTF-8}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <sourceEncoding>UTF-8</sourceEncoding>
   * }</pre>
   */
  @Parameter(defaultValue = "UTF-8")
  private String sourceEncoding;

  private void initDefaults() {
    if (includes.isEmpty()) {
      includes.add("**/*.scala.*");
    }
    if (templateFormats.isEmpty()) {
      templateFormats = DEFAULT_TEMPLATE_FORMATS;
    }
    templateImports.addAll(TwirlCompiler.DEFAULT_IMPORTS);
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    initDefaults();

    getLog().info("Twirl Templates directory: " + getSourceDirectory());
    getLog().info("Twirl Generated Sources directory: " + getOutputDirectory());
    getLog().info("Twirl Include Filters: " + prettyString(includes));
    getLog().info("Twirl Exclude Filters: " + prettyString(excludes));
    getLog()
        .info(
            "Twirl Template Formats: "
                + prettyString(
                    templateFormats.entrySet().stream()
                        .map(it -> it.getKey() + " to " + it.getValue())
                        .collect(toList())));
    getLog().info("Twirl Template Imports: " + prettyString(templateImports));
    getLog().info("Twirl Constructor Annotations: " + prettyString(constructorAnnotations));
    getLog().info("Twirl Source Encoding: " + sourceEncoding);

    final var templates = findTwirlTemplates();
    if (templates.isEmpty()) {
      getLog().info("Twirl templates to compile weren't found.");
      return;
    }

    for (File file : templates) {
      final var format = templateFormats.get(getExtension(file.getName()));
      if (format == null) {
        throw new MojoFailureException(
            String.format(
                "Unknown file format of '%s'. Possible extentions: [%s]",
                file.getName(), prettyString(templateFormats.keySet())));
      }
      if (getLog().isDebugEnabled()) {
        getLog().debug("Compile file: " + file);
      }
      TwirlCompiler.compile(
          file,
          getSourceDirectory(),
          getOutputDirectory(),
          format,
          templateImports,
          new ArrayList<>(constructorAnnotations),
          Codec.string2codec(sourceEncoding),
          false);
    }
  }

  private String prettyString(Collection<String> collection) {
    return collection.stream().collect(joining(", ", "[", "]"));
  }

  private Collection<File> findTwirlTemplates() {
    final var scanner = new DirectoryScanner();
    scanner.setIncludes(includes.toArray(new String[0]));
    scanner.setExcludes(excludes.toArray(new String[0]));
    scanner.addDefaultExcludes();

    scanner.setBasedir(getSourceDirectory());
    scanner.scan();

    return stream(scanner.getIncludedFiles())
        .map(path -> new File(getSourceDirectory(), path))
        .collect(toList());
  }
}
