#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    CREATE DATABASE logdb;
    CREATE DATABASE coordinator_db;
    CREATE DATABASE hints_db;
EOSQL
