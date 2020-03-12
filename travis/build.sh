#!/bin/sh
set -e

./gradlew assemble --info -Pprod
./gradlew sonarqube

if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
  ./gradlew bootJar -Pprod
  bash ./travis/docker_build_and_push.sh
fi
