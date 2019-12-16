#!/usr/bin/env bash

set -e
set -o pipefail
            
sbt ";++$TRAVIS_SCALA_VERSION ;validateCode ;mimaReportBinaryIssues" 
