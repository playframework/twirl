#!/usr/bin/env bash

# Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>

set -e
set -o pipefail

pushd docs 
    sbt evaluateSbtFiles validateDocs headerCheck test:headerCheck test
popd
