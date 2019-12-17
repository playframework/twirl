#!/usr/bin/env bash

sbt ";++$TRAVIS_SCALA_VERSION ;validateCode ;mimaReportBinaryIssues" || exit 1
