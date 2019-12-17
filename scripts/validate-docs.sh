#!/usr/bin/env bash

# Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>

pushd docs || exit 1
    sbt evaluateSbtFiles validateDocs headerCheck test:headerCheck test || exit 1
popd
