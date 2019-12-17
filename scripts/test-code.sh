#!/usr/bin/env bash

# Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>

sbt "++ $TRAVIS_SCALA_VERSION test" || exit 1          
sbt +publishLocal plugin/test plugin/scripted || exit 1