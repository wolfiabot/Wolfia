#!/bin/bash
set -e

while ! psql -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_roles WHERE rolname='$POSTGRES_USER'" | grep -q 1; do
  echo "Waiting on postgres own initial setup to finish"
  sleep 1
done
sleep 1
while ! pg_isready -U "$POSTGRES_USER"; do
  echo "Waiting on postgres to be ready"
  sleep 1
done

# make sure the role exists
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_roles WHERE rolname='$ROLE'" | grep -q 1 || createuser -U "$POSTGRES_USER" "$ROLE"

# make sure the database exists
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -tc "SELECT 1 FROM pg_database WHERE datname = '$DB';" | grep -q 1 || psql -U "$POSTGRES_USER" -c "CREATE DATABASE $DB WITH OWNER = $ROLE;"
# make sure the database is owned by the role
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "ALTER DATABASE $DB OWNER TO $ROLE;"
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "GRANT ALL PRIVILEGES ON DATABASE $DB TO $ROLE;"
# make sure HSTORE extension is enabled
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$DB" -c "CREATE EXTENSION IF NOT EXISTS hstore;"
