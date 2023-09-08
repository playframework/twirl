/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle.internal;

import java.io.File;
import java.util.Collection;
import java.util.List;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.workers.WorkAction;
import play.japi.twirl.compiler.TwirlCompiler;
import scala.io.Codec;

/** Gradle work action that compile one Twirl template. */
public abstract class TwirlCompileAction implements WorkAction<TwirlCompileParams> {

  private static final Logger LOGGER = Logging.getLogger(TwirlCompileAction.class);

  @Override
  public void execute() {
    try {
      File sourceFile = getParameters().getSourceFile().getAsFile().get();
      File sourceDirectory = getParameters().getSourceDirectory().getAsFile().get();
      File destinationDirectory = getParameters().getDestinationDirectory().getAsFile().get();
      String formatterType = getParameters().getFormatterType().get();
      getParameters().getTemplateImports().addAll(TwirlCompiler.DEFAULT_IMPORTS);
      Collection<String> imports = getParameters().getTemplateImports().get();
      List<String> constructorAnnotations = getParameters().getConstructorAnnotations().get();
      String sourceEncoding = getParameters().getSourceEncoding().get();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Compile Twirl template [{}/{}] {} from {} into {}",
            formatterType,
            sourceEncoding,
            sourceFile.getName(),
            sourceDirectory.getCanonicalPath(),
            destinationDirectory.getCanonicalPath());
      }
      TwirlCompiler.compile(
          sourceFile,
          sourceDirectory,
          destinationDirectory,
          formatterType,
          imports,
          constructorAnnotations,
          Codec.string2codec(sourceEncoding),
          false);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
