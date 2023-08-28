/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import java.io.File;
import javax.inject.Inject;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import play.twirl.gradle.internal.TwirlCompileAction;

public abstract class TwirlCompile extends SourceTask {

  @InputFiles
  public abstract ConfigurableFileCollection getTwirlClasspath();

  @OutputDirectory
  public abstract DirectoryProperty getDestinationDirectory();

  @Inject
  public abstract WorkerExecutor getWorkerExecutor();

  @TaskAction
  void compile() {
    WorkQueue workQueue =
        getWorkerExecutor()
            .classLoaderIsolation(
                workerSpec -> {
                  workerSpec.getClasspath().from(getTwirlClasspath());
                });

    for (File sourceFile : getSource().getFiles()) {
      workQueue.submit(
          TwirlCompileAction.class,
          parameters -> {
            parameters.getSourceFile().set(sourceFile);
            parameters.getDestinationDirectory().set(getDestinationDirectory());
          });
    }
  }
}
