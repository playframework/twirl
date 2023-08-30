/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.file.RelativeFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.FileUtils;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import play.twirl.gradle.internal.TwirlCompileAction;

public abstract class TwirlCompile extends SourceTask {

  @InputFiles
  public abstract ConfigurableFileCollection getTwirlClasspath();

  @OutputDirectory
  public abstract DirectoryProperty getDestinationDirectory();

  @Input
  public abstract MapProperty<String, String> getTemplateFormats();

  @Input
  public abstract SetProperty<String> getTemplateImports();

  @Input
  public abstract ListProperty<String> getConstructorAnnotations();

  @Input
  public abstract Property<String> getSourceEncoding();

  @Inject
  public abstract WorkerExecutor getWorkerExecutor();

  @TaskAction
  void compile() {
    WorkQueue workQueue =
        getWorkerExecutor()
            .classLoaderIsolation(spec -> spec.getClasspath().from(getTwirlClasspath()));

    Map<String, String> templateFormats = getTemplateFormats().get();
    for (RelativeFile sourceFile : getSourceAsRelativeFiles()) {
      workQueue.submit(
          TwirlCompileAction.class,
          parameters -> {
            parameters.getSourceFile().set(sourceFile.getFile());
            parameters.getSourceDirectory().set(sourceFile.getBaseDir());
            parameters.getDestinationDirectory().set(getDestinationDirectory());
            parameters
                .getFormatterType()
                .set(getFormatterType(templateFormats, sourceFile.getFile()));
            parameters.getTemplateImports().set(getTemplateImports());
            parameters.getConstructorAnnotations().set(getConstructorAnnotations());
            parameters.getSourceEncoding().set(getSourceEncoding());
          });
    }
  }

  private String getFormatterType(Map<String, String> formats, File file) {
    return formats.keySet().stream()
        .filter(ext -> FileUtils.hasExtensionIgnoresCase(file.getName(), ext))
        .findFirst()
        .orElseThrow(
            () ->
                new GradleException(
                    String.format(
                        "Unknown template format of '%s'. Possible extentions: [%s]",
                        file.getName(), String.join(", ", formats.keySet()))));
  }

  private Iterable<RelativeFile> getSourceAsRelativeFiles() {
    List<RelativeFile> relativeFiles = new ArrayList<>();
    getSource()
        .visit(
            fvd -> {
              if (fvd.getFile().isFile()) {
                relativeFiles.add(new RelativeFile(fvd.getFile(), fvd.getRelativePath()));
              }
            });
    return relativeFiles;
  }
}
