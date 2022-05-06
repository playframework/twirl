#!/usr/bin/env bash

# Copyright (C) Lightbend Inc. <https://www.lightbend.com>

sbt "++$MATRIX_SCALA test" || exit 1
sbt +publishLocal plugin/test plugin/scripted || exit 1
