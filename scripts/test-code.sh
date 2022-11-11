#!/usr/bin/env bash

# Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

sbt "++$MATRIX_SCALA test" || exit 1
sbt +publishLocal plugin/test plugin/scripted || exit 1
