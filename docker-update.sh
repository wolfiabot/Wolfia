#!/usr/bin/env bash

# this can be set up as a (daily) cron job like this:
# 0  9  *   *   *     cd /home/napster/wolfia && ./docker-update.sh &>> logs/cron.log

echo $(date)
docker-compose pull
docker-compose up -d --timeout 7300 --remove-orphans #make sure to adjust this to the value in the shutdown hook
docker image prune --force
