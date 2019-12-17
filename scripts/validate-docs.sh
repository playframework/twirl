#!/usr/bin/env bash

pushd docs || exit 1
    sbt evaluateSbtFiles validateDocs headerCheck test:headerCheck test || exit 1
popd
