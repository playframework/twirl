#!/usr/bin/env bash

# Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>

sbt ";+validateCode;+mimaReportBinaryIssues" || exit 1
