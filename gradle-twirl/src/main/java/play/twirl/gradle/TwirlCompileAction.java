/*
* Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
*/
package play.twirl.gradle;

import org.gradle.workers.WorkAction;

public abstract class TwirlCompileAction implements WorkAction<TwirlCompileParams> {

  @Override
  public void execute() {
    try {
      System.out.println(
          "Compile Twirl template " + getParameters().getSourceFile().getAsFile().get().getName());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
