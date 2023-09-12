/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import java.io.File;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.RelativeFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.FileUtils;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import play.twirl.gradle.internal.TwirlCompileAction;

/** Gradle task for compiling Twirl templates into Scala code. */
@CacheableTask
public abstract class TwirlCompile extends DefaultTask {

  @InputFiles
  @Incremental
  @IgnoreEmptyDirectories
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getSource();

  @Classpath
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
  void compile(InputChanges changes) {
    for (FileChange change : changes.getFileChanges(getSource())) {
      if (change.getFileType() == FileType.DIRECTORY) continue;
      WorkQueue workQueue =
          getWorkerExecutor()
              .classLoaderIsolation(spec -> spec.getClasspath().from(getTwirlClasspath()));

      Map<String, String> templateFormats = getTemplateFormats().get();
      RelativeFile sourceFile =
          new RelativeFile(change.getFile(), RelativePath.parse(true, change.getNormalizedPath()));
      workQueue.submit(
          TwirlCompileAction.class,
          parameters -> {
            parameters.getChangeType().set(change.getChangeType());
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
        .map(formats::get)
        .orElseThrow(
            () ->
                new GradleException(
                    String.format(
                        "Unknown template format of '%s'. Possible extentions: [%s]",
                        file.getName(), String.join(", ", formats.keySet()))));
  }
}
