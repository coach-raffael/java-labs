#!/usr/bin/env bash
# Starts shelfy against PostgreSQL with a wrong password. Read why it fails.
set -u
cd "$(dirname "$0")/../shelfy" || exit 1
export DATABASE_URL="${DATABASE_URL:-jdbc:postgresql://localhost:5432/shelfy}"
export DATABASE_USER="${DATABASE_USER:-shelfy}"
export DATABASE_PASSWORD="wrong-password"
PORT=8080 java -cp 'target/classes:target/dependency/*' shelfy.Main serve
