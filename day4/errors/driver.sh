#!/usr/bin/env bash
# Starts shelfy against PostgreSQL with target/dependency missing from the classpath. Read why it fails.
# (Jackson is missing too, but the database connection comes first in Main, so the driver error is what you see.)
set -u
cd "$(dirname "$0")/../shelfy" || exit 1
export DATABASE_URL="${DATABASE_URL:-jdbc:postgresql://localhost:5432/shelfy}"
export DATABASE_USER="${DATABASE_USER:-shelfy}"
export DATABASE_PASSWORD="${DATABASE_PASSWORD:-shelfy-lab}"
PORT=8080 java -cp target/classes shelfy.Main serve
