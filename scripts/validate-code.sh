#!/usr/bin/env bash

# Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>

sbt -Dsbt013=true ";+validateCode;+mimaReportBinaryIssues" || exit 1
sbt -Dsbt013=false ";+validateCode;+mimaReportBinaryIssues" || exit 1
