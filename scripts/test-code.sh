#!/usr/bin/env bash

sbt "++ $TRAVIS_SCALA_VERSION test"           || exit 1
sbt +publishLocal plugin/test plugin/scripted || exit 1
