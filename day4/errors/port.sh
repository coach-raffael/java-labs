#!/usr/bin/env bash
# Starts shelfy twice on the same port. The second start fails; read why.
set -u
cd "$(dirname "$0")/../shelfy" || exit 1
CP='target/classes:target/dependency/*'
PORT=8080 java -cp "$CP" shelfy.Main --storage memory serve &
FIRST=$!
sleep 2
echo
echo "---- second start on the same port:"
PORT=8080 java -cp "$CP" shelfy.Main --storage memory serve
echo
echo "---- stopping the first one (pid $FIRST)"
kill "$FIRST"
