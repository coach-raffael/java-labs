#!/usr/bin/env bash
# Lab Module 18, Part A: from the JAR to an installable package. Fill in the four steps.
# Module 17 shows each command; this file only names the steps, the options are yours to find.
# Run after ./mvnw verify from this directory:  ./package.sh
set -eu
cd "$(dirname "$0")"
JAR=target/dist/shelfy-2.3.0.jar
if [ ! -f "$JAR" ]; then
  echo "Build the executable JAR first (Module 17.1): ./mvnw verify must produce $JAR"
  exit 1
fi

echo "== 1. Which modules does shelfy need? (jdeps)"
# TODO: jdeps ... --print-module-deps ... "$JAR" target/dist/lib/*.jar

echo "== 2. A runtime with exactly these modules (jlink)"
# TODO: jlink --add-modules <the list from step 1> ... --output target/runtime

echo "== 3. An application image with that runtime (jpackage --type app-image)"
# TODO: jpackage --type app-image --input target/dist --main-jar shelfy-2.3.0.jar --main-class shelfy.Main ... --dest target/pkg

echo "== 4. A native package for this machine (jpackage --type rpm or deb, from the app image)"
# TODO: jpackage --type rpm|deb --app-image target/pkg/shelfy ... --dest target/pkg
