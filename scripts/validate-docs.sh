#!/usr/bin/env bash

# Copyright (C) Lightbend Inc. <https://www.lightbend.com>

pushd docs || exit 1
    sbt evaluateSbtFiles validateDocs headerCheck test:headerCheck test || exit 1
popd
