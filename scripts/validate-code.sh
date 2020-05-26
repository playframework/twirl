#!/usr/bin/env bash

# Copyright (C) Lightbend Inc. <https://www.lightbend.com>

sbt ";+validateCode;+mimaReportBinaryIssues" || exit 1
