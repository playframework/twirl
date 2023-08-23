/*
* Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
*/
package play.twirl.gradle;

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
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

  private final ObjectFactory objectFactory;

  @Inject
  public TwirlPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void apply(final Project project) {
    project.getPluginManager().apply(ScalaBasePlugin.class);

    configureSourceSetDefaults(project);
  }

  private void configureSourceSetDefaults(final Project project) {
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
              createTwirlCompileTask(project, sourceSet, twirlSource);
            });
  }

  private void createTwirlCompileTask(
      final Project project, final SourceSet sourceSet, TwirlSourceDirectorySet twirlSource) {
    final TaskProvider<TwirlCompile> twirlTask =
        project
            .getTasks()
            .register(
                sourceSet.getCompileTaskName("twirl"),
                TwirlCompile.class,
                twirlCompile -> {
                  twirlCompile.setDescription("Compiles the " + twirlSource + ".");
                  twirlCompile.setSource(twirlSource);
                  twirlCompile
                      .getDestinationDirectory()
                      .convention(project.getLayout().getBuildDirectory())
                      .dir(
                          "generated/sources/" + twirlSource.getName() + "/" + sourceSet.getName());
                });
  }

  private TwirlSourceDirectorySet getTwirlSourceDirectorySet(SourceSet sourceSet) {
    String displayName = ((DefaultSourceSet) sourceSet).getDisplayName();
    TwirlSourceDirectorySet twirlSourceDirectorySet =
        objectFactory.newInstance(
            DefaultTwirlSourceDirectorySet.class,
            objectFactory.sourceDirectorySet("twirl", displayName + " Twirl source"));
    twirlSourceDirectorySet.getFilter().include("**/*.scala.*");
    return twirlSourceDirectorySet;
  }

  private static JavaPluginExtension javaPluginExtension(Project project) {
    return extensionOf(project, JavaPluginExtension.class);
  }

  private static <T> T extensionOf(ExtensionAware extensionAware, Class<T> type) {
    return extensionAware.getExtensions().getByType(type);
  }
}
