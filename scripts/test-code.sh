#!/usr/bin/env bash

# Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

sbt "++$MATRIX_SCALA test; mavenPlugin/scripted" || exit 1
sbt +publishLocal +compiler/publishM2 +apiJVM/publishM2 +plugin/test plugin/scripted || exit 1
(cd gradle-twirl && ./gradlew clean check -x spotlessCheck --no-daemon -Pscala.version="$MATRIX_SCALA") || exit 1
