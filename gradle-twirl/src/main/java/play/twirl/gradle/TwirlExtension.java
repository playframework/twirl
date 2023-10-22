/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import org.gradle.api.provider.Property;

/**
 * The extension of the plugin allowing for configuring the target Scala version used for the
 * application.
 */
public abstract class TwirlExtension {

  /**
   * Scala version used for compilation Twirl templates.
   *
   * <pre>{@code
   * twirl {
   *   scalaVersion.set("3")
   * }
   * }</pre>
   */
  public abstract Property<String> getScalaVersion();
}
