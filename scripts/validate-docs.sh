#!/usr/bin/env bash

pushd docs || travis_terminate 1
    sbt evaluateSbtFiles validateDocs headerCheck test:headerCheck test || travis_terminate 1
popd
