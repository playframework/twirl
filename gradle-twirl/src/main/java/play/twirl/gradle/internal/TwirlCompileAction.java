/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.twirl.gradle.internal;

import java.io.File;
import java.util.Collection;
import java.util.List;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.work.ChangeType;
import org.gradle.workers.WorkAction;
import play.japi.twirl.compiler.TwirlCompiler;
import play.twirl.compiler.TwirlCompiler$;
import scala.io.Codec;

/** Gradle work action that compile or delete one Twirl template. */
public abstract class TwirlCompileAction implements WorkAction<TwirlCompileParams> {

  private static final Logger LOGGER = Logging.getLogger(TwirlCompileAction.class);

  @Override
  public void execute() {
    if (getParameters().getChangeType().get() == ChangeType.REMOVED) {
      delete();
    } else {
      compile();
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void delete() {
    try {
      File sourceFile = getParameters().getSourceFile().getAsFile().get();
      File sourceDirectory = getParameters().getSourceDirectory().getAsFile().get();
      File destinationDirectory = getParameters().getDestinationDirectory().getAsFile().get();
      String sourceEncoding = getParameters().getSourceEncoding().get();
      // WA: Need to create a source file temporarily for correct calculate path of compiled
      // template to delete
      sourceFile.createNewFile();
      File compiledTemplate =
          TwirlCompiler$.MODULE$
              .generatedFile(
                  sourceFile,
                  Codec.string2codec(sourceEncoding),
                  sourceDirectory,
                  destinationDirectory,
                  false)
              ._2
              .file();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Delete Twirl template {}", compiledTemplate.getCanonicalPath());
      }
      // Delete temporary empty source file
      sourceFile.delete();
      compiledTemplate.delete();
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private void compile() {
    try {
      File sourceFile = getParameters().getSourceFile().getAsFile().get();
      File sourceDirectory = getParameters().getSourceDirectory().getAsFile().get();
      File destinationDirectory = getParameters().getDestinationDirectory().getAsFile().get();
      String formatterType = getParameters().getFormatterType().get();
      String extension = getParameters().getFormatExtension().get();
      getParameters().getTemplateImports().addAll(TwirlCompiler.DEFAULT_IMPORTS);
      Collection<String> imports = getParameters().getTemplateImports().get();
      List<String> constructorAnnotations = getParameters().getConstructorAnnotations().get();
      String sourceEncoding = getParameters().getSourceEncoding().get();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
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
          TwirlCompiler.formatImports(imports, extension),
          constructorAnnotations,
          Codec.string2codec(sourceEncoding),
          false);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
