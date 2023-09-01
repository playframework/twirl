/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/**
 * A {@code TwirlSourceDirectorySet} defines the properties and methods added to a {@link
 * org.gradle.api.tasks.SourceSet} by the {@code TwirlPlugin}.
 */
public interface TwirlSourceDirectorySet extends SourceDirectorySet {

  MapProperty<String, String> getTemplateFormats();

  SetProperty<String> getTemplateImports();

  ListProperty<String> getConstructorAnnotations();

  Property<String> getSourceEncoding();
}