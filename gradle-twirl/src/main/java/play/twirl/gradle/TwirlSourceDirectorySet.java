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

  /**
   * Custom template formats configured for this source directory set.
   *
   * <pre>{@code
   * sourceSets {
   *   main {
   *     twirl {
   *       templateFormats.put("csv", "play.twirl.api.TxtFormat")
   *     }
   *   }
   * }
   * }</pre>
   */
  MapProperty<String, String> getTemplateFormats();

  /**
   * Imports that should be added to generated source files.
   *
   * <pre>{@code
   * sourceSets {
   *   main {
   *     twirl {
   *       templateImports.add("org.example._")
   *     }
   *   }
   * }
   * }</pre>
   */
  SetProperty<String> getTemplateImports();

  /**
   * Annotations added to constructors in injectable templates.
   *
   * <pre>{@code
   * sourceSets {
   *   main {
   *     twirl {
   *       constructorAnnotations.add("@org.example.MyAnnotation()")
   *     }
   *   }
   * }
   * }</pre>
   */
  ListProperty<String> getConstructorAnnotations();

  /**
   * Source encoding for template files and generated scala files.
   *
   * <pre>{@code
   * sourceSets {
   *   main {
   *     twirl {
   *       sourceEncoding.set("<enc>")
   *     }
   *   }
   * }
   * }</pre>
   */
  Property<String> getSourceEncoding();
}
