#!/usr/bin/env bash

# Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

# The sbt plugin is compiled separately with the Scala version used by sbt itself.
# Do not place an experimental Scala Next library on that compiler's classpath.
sbt -v --server -Dscala.version="$MATRIX_SCALA" \
  "apiJVM/testFull; apiJS/testFull; parser/testFull; compiler/testFull; mavenPlugin/testFull; mavenPlugin/scripted" || exit 1
sbt shutdown || exit 1
twirl_version="$(sed -n 's/^twirl.compiler.version=//p' compiler/version.properties)"
if [[ -z "$twirl_version" ]]; then
  echo "Could not read twirl.compiler.version from compiler/version.properties" >&2
  exit 1
fi
version_option=(-Dproject.version="$twirl_version")
sbt -v --server "${version_option[@]}" -Dscripted.scala.version="$MATRIX_SCALA" +publishLocal +compiler/publishM2 +apiJVM/publishM2 "++2.12.x; plugin/testFull; plugin/scripted" || exit 1
sbt shutdown || exit 1
# Run Gradle while compiler/version.properties still matches the artifacts published by the preceding sbt invocation.
(cd gradle-twirl && ./gradlew clean check -x spotlessCheck --no-daemon -Pscala.version="$MATRIX_SCALA") || exit 1
# A fresh load restores the canonical Scala 3.3 compiler projects before the Scala 3.9 sbt plugin is tested.
sbt -v --server "${version_option[@]}" -Dscripted.scala.version="$MATRIX_SCALA" "plugin/testFull; plugin/scripted" || exit 1
