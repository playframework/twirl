/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.lambdas.SerializableLambdas;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.scala.ScalaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import play.twirl.gradle.internal.DefaultTwirlSourceDirectorySet;

/** A simple 'hello world' plugin. */
public class TwirlPlugin implements Plugin<Project> {

  private static final String DEFAULT_SCALA_VERSION = "2.13";

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

  private final ObjectFactory objectFactory;

  @Inject
  public TwirlPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void apply(final Project project) {
    project.getPluginManager().apply(ScalaBasePlugin.class);

    TwirlExtension twirlExtension = project.getExtensions().create("twirl", TwirlExtension.class);
    twirlExtension.getScalaVersion().convention(DEFAULT_SCALA_VERSION);

    Configuration twirlConfiguration = createDefaultTwirlConfiguration(project, twirlExtension);

    configureSourceSetDefaults(project, twirlConfiguration);
  }

  private Configuration createDefaultTwirlConfiguration(
      Project project, TwirlExtension twirlExtension) {
    // Get Twirl version from Gradle Plugin MANIFEST.MF
    String twirlVersion = getClass().getPackage().getImplementationVersion();
    Configuration conf = project.getConfigurations().create("twirl");
    conf.setDescription("The Twirl compiler library.");
    conf.setVisible(false);
    conf.setTransitive(true);
    project.afterEvaluate(
        __ -> {
          Dependency twirlCompiler =
              project
                  .getDependencies()
                  .create(
                      String.format(
                          "com.typesafe.play:twirl-compiler_%s:%s",
                          twirlExtension.getScalaVersion().get(), twirlVersion));
          conf.defaultDependencies(dependencies -> dependencies.add(twirlCompiler));
        });
    return conf;
  }

  private void configureSourceSetDefaults(
      final Project project, final Configuration twirlConfiguration) {
    javaPluginExtension(project)
        .getSourceSets()
        .all(
            (sourceSet) -> {
              TwirlSourceDirectorySet twirlSource = getTwirlSourceDirectorySet(sourceSet);
              sourceSet.getExtensions().add(TwirlSourceDirectorySet.class, "twirl", twirlSource);
              twirlSource.srcDir(project.file("src/" + sourceSet.getName() + "/twirl"));
              sourceSet
                  .getResources()
                  .getFilter()
                  .exclude(
                      SerializableLambdas.spec(
                          (element) -> twirlSource.contains(element.getFile())));
              sourceSet.getAllJava().source(twirlSource);
              sourceSet.getAllSource().source(twirlSource);
              createTwirlCompileTask(project, sourceSet, twirlSource, twirlConfiguration);
            });
  }

  private void createTwirlCompileTask(
      final Project project,
      final SourceSet sourceSet,
      TwirlSourceDirectorySet twirlSource,
      final Configuration twirlConfiguration) {
    final TaskProvider<TwirlCompile> twirlTask =
        project
            .getTasks()
            .register(
                sourceSet.getCompileTaskName("twirl"),
                TwirlCompile.class,
                twirlCompile -> {
                  twirlCompile.setDescription("Compiles the " + twirlSource + ".");
                  twirlCompile.getTwirlClasspath().setFrom(twirlConfiguration);
                  twirlCompile.setSource(twirlSource);
                  twirlCompile.getTemplateFormats().convention(twirlSource.getTemplateFormats());
                  twirlCompile.getTemplateImports().convention(twirlSource.getTemplateImports());
                  twirlCompile.getSourceEncoding().convention(twirlSource.getSourceEncoding());
                  twirlCompile
                      .getConstructorAnnotations()
                      .convention(twirlSource.getConstructorAnnotations());
                  DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();
                  twirlCompile
                      .getDestinationDirectory()
                      .convention(
                          buildDirectory.dir(
                              "generated/sources/"
                                  + twirlSource.getName()
                                  + "/"
                                  + sourceSet.getName()));
                });
  }

  private TwirlSourceDirectorySet getTwirlSourceDirectorySet(SourceSet sourceSet) {
    String displayName = ((DefaultSourceSet) sourceSet).getDisplayName();
    TwirlSourceDirectorySet twirlSourceDirectorySet =
        objectFactory.newInstance(
            DefaultTwirlSourceDirectorySet.class,
            objectFactory.sourceDirectorySet("twirl", displayName + " Twirl source"));
    twirlSourceDirectorySet.getFilter().include("**/*.scala.*");
    twirlSourceDirectorySet.getTemplateFormats().convention(DEFAULT_TEMPLATE_FORMATS);
    return twirlSourceDirectorySet;
  }

  private static JavaPluginExtension javaPluginExtension(Project project) {
    return extensionOf(project, JavaPluginExtension.class);
  }

  private static <T> T extensionOf(ExtensionAware extensionAware, Class<T> type) {
    return extensionAware.getExtensions().getByType(type);
  }
}
