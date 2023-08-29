/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle.internal;

import javax.inject.Inject;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import play.twirl.gradle.TwirlSourceDirectorySet;

public class DefaultTwirlSourceDirectorySet extends DefaultSourceDirectorySet
    implements TwirlSourceDirectorySet {

  private final MapProperty<String, String> templateFormats;

  @Inject
  public DefaultTwirlSourceDirectorySet(
      SourceDirectorySet sourceDirectorySet,
      TaskDependencyFactory taskDependencyFactory,
      ObjectFactory objectFactory) {
    super(sourceDirectorySet, taskDependencyFactory); // Gradle 8+
    this.templateFormats = objectFactory.mapProperty(String.class, String.class);
  }

  @Override
  public MapProperty<String, String> getTemplateFormats() {
    return templateFormats;
  }
}
