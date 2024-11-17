#!/usr/bin/env bash

# this can be set up as a (daily) cron job on the maching hosting the wolfia container like this:
# 0  9  *   *   *     cd /home/foo/wolfia && ./docker-update.sh &>> logs/cron.log

echo $(date)
docker compose pull
docker compose up -d --remove-orphans
docker image prune --force
