#!/usr/bin/env bash

sbt ++$TRAVIS_SCALA_VERSION validateCode      || travis_terminate 1
sbt "++ $TRAVIS_SCALA_VERSION test"           || travis_terminate 1
sbt +publishLocal plugin/test plugin/scripted || travis_terminate 1