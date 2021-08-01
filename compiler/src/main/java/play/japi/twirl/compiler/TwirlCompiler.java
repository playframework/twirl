/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package play.japi.twirl.compiler;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import scala.Option;
import scala.collection.Seq;
import scala.io.Codec;
import scala.util.Properties$;

// when we drop Scala 2.12, we can replace collection.JavaConverters with
// collection.jdk.CollectionConverters and get rid of this
@SuppressWarnings("deprecation")

public class TwirlCompiler {

  public static final Set<String> DEFAULT_IMPORTS;
  static {
    Set<String> imports = new HashSet<>();
    imports.addAll(scala.collection.JavaConverters$.MODULE$
        .seqAsJavaListConverter(play.twirl.compiler.TwirlCompiler$.MODULE$.DefaultImports()).asJava());
    DEFAULT_IMPORTS = Collections.unmodifiableSet(imports);
  }

  public static Optional<File> compile(File source, File sourceDirectory, File generatedDirectory, String formatterType,
      Collection<String> additionalImports, List<String> constructorAnnotations) {
    Charset sourceEncoding = Charset.forName(Properties$.MODULE$.sourceEncoding());
    return compile(source, sourceDirectory, generatedDirectory, formatterType, additionalImports,
        constructorAnnotations, new Codec(sourceEncoding), false);
  }

  public static Optional<File> compile(File source, File sourceDirectory, File generatedDirectory, String formatterType,
      Collection<String> additionalImports, List<String> constructorAnnotations, Codec codec, boolean inclusiveDot) {
    Seq<String> scalaAdditionalImports = scala.collection.JavaConverters$.MODULE$.asScalaBufferConverter(new ArrayList<String>(additionalImports)).asScala();
    Seq<String> scalaConstructorAnnotations = scala.collection.JavaConverters$.MODULE$.asScalaBufferConverter(constructorAnnotations).asScala();

    Option<File> option = play.twirl.compiler.TwirlCompiler.compile(source, sourceDirectory, generatedDirectory,
        formatterType, scalaAdditionalImports, scalaConstructorAnnotations, codec, inclusiveDot);
    return Optional.ofNullable(option.nonEmpty() ? option.get() : null);
  }

}
