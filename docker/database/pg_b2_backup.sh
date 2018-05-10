#!/bin/bash
# thx hcg randon for parts of this script (root and file checks) :eyes:
# this requires the backblaze CLI tool to be installed (pip install b2)

# pass 5 args:
#  - database name (needs to exist in postgres)
#  - app name (meta information)
#  - bucket name (target of the upload)
#  - backblaze account id
#  - backblaze app key
#
# example: ./pg_b2_backup.sh wolfia wolfia backups-daily foo bar

set -e
echo $(date)

# Verify we are root
if [ "$EUID" -ne 0 ]; then
 echo "Please run as root"
 exit 1
fi

DB=$1
APP=$2
BUCKET=$3
B2_ACCOUNT_ID=$4
B2_APP_KEY=$5
echo "Backing up db ${DB} of app ${APP} to bucket ${BUCKET}."

DUMPDIR="/tmp"

#will look like: app_db_2017-12-31.dump
FILENAME=${APP}_${DB}_$(date +%Y-%m-%d).dump
INFO="--info app=${APP} --info db=${DB}"

mkdir -p ${DUMPDIR}

# cleanup any old backups
if [ -f "${DUMPDIR}/${FILENAME}" ]; then
  rm -f "${DUMPDIR}/${FILENAME}"
fi

# dump it
su - postgres -c "pg_dump ${DB} > ${DUMPDIR}/${FILENAME}"

# calculate sha1 sum
SHA1=$(sha1sum ${DUMPDIR}/${FILENAME} | sed -En "s/^([0-9a-f]{40}).*/\1/p")

#log in to backblaze
b2 authorize-account ${B2_ACCOUNT_ID} ${B2_APP_KEY}

# upload it
b2 upload-file --sha1 ${SHA1} \
  ${INFO} \
  --noProgress \
  ${BUCKET} \
  ${DUMPDIR}/${FILENAME} \
  ${FILENAME}

#log out
b2 clear-account


# make sure file still exists and clean it up
if [ -f "${DUMPDIR}/${FILENAME}" ]; then
  rm -f "${DUMPDIR}/${FILENAME}"
fi
