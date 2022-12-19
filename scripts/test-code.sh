#!/usr/bin/env bash

# Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>

sbt -Dsbt013=true "++ 2.10.7 test" || exit 1
sbt -Dsbt013=false "++ 2.12.17 test" || exit 1
sbt -Dsbt013=false "++ 2.13.10 test" || exit 1
sbt -Dsbt013=true "+publishLocal ; plugin/test" || exit 1
sbt -Dsbt013=false "+publishLocal ; plugin/test" || exit 1

run_scripted () {
  sbt -Dsbt013=$SBT_013 "
    project plugin;
    set scriptedSbt := \"$MATRIX_SBT\";
    set scriptedLaunchOpts += \"-Dscala.version=$MATRIX_SCALA\";
    set scriptedLaunchOpts += \"-Dsbt013=$SBT_013\";
    show scriptedSbt;
    show scriptedLaunchOpts;
    scripted
  "
}

SBT_013=true ; MATRIX_SBT=0.13.18 ; MATRIX_SCALA=2.10.7
run_scripted || exit 1
SBT_013=false ; MATRIX_SBT=1.3.13 ; MATRIX_SCALA=2.12.17
run_scripted || exit 1
SBT_013=false ; MATRIX_SBT=1.8.0 ; MATRIX_SCALA=2.12.17
run_scripted || exit 1
SBT_013=false ; MATRIX_SBT=1.3.13 ; MATRIX_SCALA=2.13.10
run_scripted || exit 1
SBT_013=false ; MATRIX_SBT=1.8.0 ; MATRIX_SCALA=2.13.10
run_scripted || exit 1
