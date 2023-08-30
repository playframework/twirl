/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle;

import org.gradle.api.provider.Property;

public abstract class TwirlExtension {

  public abstract Property<String> getScalaVersion();
}
