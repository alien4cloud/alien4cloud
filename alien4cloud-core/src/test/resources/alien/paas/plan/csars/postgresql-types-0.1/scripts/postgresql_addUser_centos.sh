#!/bin/bash

user=$1
passW=$2
name=$3

echo "Creating ${user} role in postgresql ..."

su - postgres -c "psql postgres -tAc \"SELECT 1 FROM pg_roles WHERE rolname='${user}'\"" | grep -q 1 || su - postgres -c "psql -c \"CREATE USER ${user} WITH PASSWORD '${passW}' CREATEDB\""
# su - postgres -c "psql -c \"CREATE USER ${user} WITH PASSWORD '${passW}' CREATEDB\""

echo "${user} role created "

echo "Creating ${name} database in postgresql ..."

su - postgres -c "psql -lqt | cut -d \| -f 1 | grep -w ${name} | wc -l" | grep -q 1 || su - postgres -c "psql -c 'CREATE DATABASE ${name} OWNER ${user}'"
# su - postgres -c "psql -c 'CREATE DATABASE ${name} OWNER ${user}'"
su - postgres -c "psql -c 'GRANT ALL PRIVILEGES ON DATABASE ${name} to ${user}'"

echo "${name} database created"
