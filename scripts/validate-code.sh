#!/usr/bin/env bash

sbt ++$TRAVIS_SCALA_VERSION validateCode             || travis_terminate 1
sbt "++$TRAVIS_SCALA_VERSION mimaReportBinaryIssues" || travis_terminate 1
