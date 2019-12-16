#!/usr/bin/env bash

set -e
set -o pipefail

pushd docs 
    sbt evaluateSbtFiles validateDocs headerCheck test:headerCheck test
popd
