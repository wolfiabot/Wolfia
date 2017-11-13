#!/usr/bin/env bash

docker-compose pull
docker-compose up -d --timeout 7300 --remove-orphans #make sure to adjust this to the value in the shutdown hook
docker image prune --force
