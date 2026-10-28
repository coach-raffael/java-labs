#!/usr/bin/env bash
# Day 4, Module 15, Part B: starts shelfy 2.2 from day4/shelfy with a planted fault.
# Read the message, find the cause with the tools of Module 14.7 (ss -ltnp, the environment, psql),
# explain it in two sentences, then make the normal start work again.
# Run from the repository root:  ./checks/day4-break.sh        (plants one of two faults at random)
#                                 ./checks/day4-break.sh --clean (removes what the script left behind)
set -u
LAB="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT="$LAB/day4/shelfy"
CP="$PROJECT/target/classes:$PROJECT/target/dependency/*"
PIDFILE="/tmp/shelfy-day4-break.pid"

if [ "${1:-}" = "--clean" ]; then
  if [ -f "$PIDFILE" ]; then
    kill "$(cat "$PIDFILE")" 2>/dev/null && echo "stopped the process from the last run"
    rm -f "$PIDFILE"
  fi
  exit 0
fi

if [ ! -d "$PROJECT/target/classes" ]; then
  echo "Build day4/shelfy first: cd day4/shelfy && ./mvnw -q verify"
  exit 1
fi

fault="${SHELFY_BREAK:-$((RANDOM % 2 + 1))}"
export DATABASE_URL="${DATABASE_URL:-jdbc:postgresql://localhost:5432/shelfy}"
export DATABASE_USER="${DATABASE_USER:-shelfy}"
export DATABASE_PASSWORD="${DATABASE_PASSWORD:-shelfy-lab}"
export PORT="${PORT:-8080}"

case "$fault" in
  1)
    # Something else already listens on the port. It stays until --clean.
    if [ ! -f "$PIDFILE" ]; then
      (cd /tmp && PORT="$PORT" exec java -cp "$CP" shelfy.Main --storage memory serve >/dev/null 2>&1) &
      echo $! >"$PIDFILE"
      sleep 2
    fi
    ;;
  2)
    export DATABASE_PASSWORD="shelfy-l4b"
    ;;
esac

echo "Starting shelfy with PORT=$PORT and DATABASE_URL=$DATABASE_URL ..."
echo
cd "$PROJECT" && java -cp "$CP" shelfy.Main serve
echo
echo "If the start failed: read the first line of the message and the 'Caused by' lines."
echo "When you have fixed it, run ./checks/day4.sh again, then ./checks/day4-break.sh --clean."
