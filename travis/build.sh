#!/bin/sh
set -e

./gradlew assemble --info
./gradlew sonarqube

if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
  ./gradlew bootJar
  bash ./travis/docker_build_and_push.sh
fi
