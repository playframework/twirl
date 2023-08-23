/*
* Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
*/
package play.twirl.gradle;

import java.io.File;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

public abstract class TwirlCompile extends SourceTask {

  @OutputDirectory
  public abstract DirectoryProperty getDestinationDirectory();

  @TaskAction
  void compile() {
    for (File sourceFile : getSource().getFiles()) {
      try {
        System.out.println("Compile Twirl template " + sourceFile.getName());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
