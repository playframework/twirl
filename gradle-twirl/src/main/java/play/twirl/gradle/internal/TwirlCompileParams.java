/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle.internal;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

public interface TwirlCompileParams extends WorkParameters {

  RegularFileProperty getSourceFile();

  DirectoryProperty getSourceDirectory();

  DirectoryProperty getDestinationDirectory();

  Property<String> getFormatterType();
}
