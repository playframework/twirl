#!/usr/bin/env bash

pushd docs
    sbt evaluateSbtFiles validateDocs headerCheck test:headerCheck test || travis_terminate 1
popd