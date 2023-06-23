/*
* Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
*/
package play.twirl.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/** A simple 'hello world' plugin. */
public class TwirlPlugin implements Plugin<Project> {
  public void apply(Project project) {
    // Register a task
    project
        .getTasks()
        .register(
            "greeting",
            task -> {
              task.doLast(s -> System.out.println("Hello from plugin 'com.playframework.twirl'"));
            });
  }
}
