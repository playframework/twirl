#!/usr/bin/env bash

# Copyright (C) Lightbend Inc. <https://www.lightbend.com>

sbt "++ $SCALA_VERSION test" || exit 1
sbt +publishLocal plugin/test plugin/scripted || exit 1
