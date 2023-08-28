/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle.internal;

import javax.inject.Inject;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import play.twirl.gradle.TwirlSourceDirectorySet;

public class DefaultTwirlSourceDirectorySet extends DefaultSourceDirectorySet
    implements TwirlSourceDirectorySet {
  @Inject
  public DefaultTwirlSourceDirectorySet(
      SourceDirectorySet sourceDirectorySet, TaskDependencyFactory taskDependencyFactory) {
    super(sourceDirectorySet, taskDependencyFactory); // Gradle 8+
  }
}
