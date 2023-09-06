/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle.internal;

import javax.inject.Inject;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;

@Deprecated
public class Gradle7TwirlSourceDirectorySet extends DefaultTwirlSourceDirectorySet {

  @Inject
  public Gradle7TwirlSourceDirectorySet(
      SourceDirectorySet sourceDirectorySet, ObjectFactory objectFactory) {
    super(sourceDirectorySet, objectFactory);
  }
}
