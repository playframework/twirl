/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.japi.twirl.compiler;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import scala.Option;
import scala.collection.JavaConverters$;
import scala.collection.Seq;
import scala.io.Codec;
import scala.util.Properties$;

public class TwirlCompiler {

  public static final Set<String> DEFAULT_IMPORTS;

  static {
    String scalaVersion = play.twirl.compiler.BuildInfo$.MODULE$.scalaVersion();
    DEFAULT_IMPORTS =
        Set.copyOf(
            toJavaList(play.twirl.compiler.TwirlCompiler$.MODULE$.defaultImports(scalaVersion)));
  }

  public static Optional<File> compile(
      File source,
      File sourceDirectory,
      File generatedDirectory,
      String formatterType,
      Collection<String> additionalImports,
      List<String> constructorAnnotations) {
    Charset sourceEncoding = Charset.forName(Properties$.MODULE$.sourceEncoding());
    return compile(
        source,
        sourceDirectory,
        generatedDirectory,
        formatterType,
        additionalImports,
        constructorAnnotations,
        new Codec(sourceEncoding),
        false);
  }

  public static Optional<File> compile(
      File source,
      File sourceDirectory,
      File generatedDirectory,
      String formatterType,
      Collection<String> additionalImports,
      List<String> constructorAnnotations,
      Codec codec,
      boolean inclusiveDot) {
    Seq<String> scalaAdditionalImports = toScalaSeq(additionalImports);
    Seq<String> scalaConstructorAnnotations = toScalaSeq(constructorAnnotations);

    Option<File> option =
        play.twirl.compiler.TwirlCompiler.compile(
            source,
            sourceDirectory,
            generatedDirectory,
            formatterType,
            scalaAdditionalImports,
            scalaConstructorAnnotations,
            codec,
            inclusiveDot);
    return Optional.ofNullable(option.nonEmpty() ? option.get() : null);
  }

  public static Collection<String> formatImports(
      Collection<String> templateImports, String extension) {
    return toJavaList(
        play.twirl.compiler.TwirlCompiler.formatImports(
            toScalaSeq(templateImports).toSeq(), extension));
  }

  private static <T> Seq<T> toScalaSeq(Collection<T> collection) {
    return JavaConverters$.MODULE$.asScalaBufferConverter(new ArrayList<>(collection)).asScala();
  }

  private static <T> List<T> toJavaList(Seq<T> seq) {
    return JavaConverters$.MODULE$.seqAsJavaListConverter(seq).asJava();
  }
}
