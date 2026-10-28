#!/usr/bin/env bash
# Day 4, Module 15: acceptance checks for "shelfy 2.2 serves HTTP".
# Expects: your Maven project in day4/shelfy and a PostgreSQL database you may use for tests:
#   TEST_DATABASE_URL (default jdbc:postgresql://localhost:5432/shelfy), TEST_DATABASE_USER (shelfy),
#   TEST_DATABASE_PASSWORD (shelfy-lab). The tables are emptied at the start.
# Run from the repository root:  ./checks/day4.sh
set -u
. "$(dirname "$0")/lib.sh"
LAB="$(cd "$(dirname "$0")/.." && pwd)"
need_java

PROJECT="${SHELFY_PROJECT:-$LAB/day4/shelfy}"
BUILD="${SHELFY_BUILD:-./mvnw -q verify}"
CP="${SHELFY_CP:-$PROJECT/target/classes:$PROJECT/target/dependency/*}"
DB_URL="${TEST_DATABASE_URL:-jdbc:postgresql://localhost:5432/shelfy}"
DB_USER="${TEST_DATABASE_USER:-shelfy}"
DB_PASSWORD="${TEST_DATABASE_PASSWORD:-shelfy-lab}"
PORT="${SHELFY_CHECK_PORT:-18084}"
WORK="$(mktemp -d)"
APP_PID=""
cleanup() {
  if [ -n "$APP_PID" ]; then
    kill "$APP_PID" 2>/dev/null
  fi
  rm -rf "$WORK"
}
trap cleanup EXIT

# psql_cmd SQL -> runs SQL against the test database (host and port taken from the JDBC URL)
PG_HOSTPORT="$(printf '%s' "$DB_URL" | sed -n 's#jdbc:postgresql://\([^/]*\)/.*#\1#p')"
PG_HOST="${PG_HOSTPORT%%:*}"
PG_PORT="${PG_HOSTPORT##*:}"
[ "$PG_PORT" = "$PG_HOST" ] && PG_PORT=5432
PG_DB="$(printf '%s' "$DB_URL" | sed -n 's#jdbc:postgresql://[^/]*/\([^?]*\).*#\1#p')"
psql_cmd() {
  PGPASSWORD="$DB_PASSWORD" psql -h "$PG_HOST" -p "$PG_PORT" -U "$DB_USER" -d "$PG_DB" -q -t -A -c "$1"
}

start_app() {
  # exec: the subshell becomes the java process, so that kill "$APP_PID" stops the app itself
  (cd "$WORK" && DATABASE_URL="$DB_URL" DATABASE_USER="$DB_USER" DATABASE_PASSWORD="$DB_PASSWORD" PORT="$PORT" \
     LOG_FORMAT=json exec java -cp "$CP" shelfy.Main serve >>"$WORK/app.log" 2>&1) &
  APP_PID=$!
  if wait_for_port "$PORT" 20; then
    ok "the app answers on port $PORT"
  else
    fail "the app did not start on port $PORT:"
    tail -n 20 "$WORK/app.log"
    summary
  fi
}

stop_app() {
  kill "$APP_PID" 2>/dev/null
  wait "$APP_PID" 2>/dev/null
  APP_PID=""
}

echo "Part A: build and database"
if (cd "$PROJECT" && TEST_DATABASE_URL="$DB_URL" TEST_DATABASE_USER="$DB_USER" TEST_DATABASE_PASSWORD="$DB_PASSWORD" \
      $BUILD >"$WORK/build.log" 2>&1); then
  ok "$BUILD ends with BUILD SUCCESS (PostgresStorageTest included)"
else
  fail "$BUILD failed:"
  tail -n 30 "$WORK/build.log"
  summary
fi
check "a test class calls /loans (the string \"/loans appears in src/test/java)" grep -rq '"/loans' "$PROJECT/src/test/java"
if ! command -v psql >/dev/null 2>&1; then
  echo "psql is not installed; the check needs it to empty the test tables (Module 14.1)."
  exit 1
fi
if psql_cmd "SELECT 1" >/dev/null 2>&1; then
  ok "psql reaches $DB_URL as $DB_USER"
else
  fail "psql cannot reach $DB_URL as $DB_USER (is PostgreSQL running, do user and password match?)"
  summary
fi
psql_cmd "DROP TABLE IF EXISTS loans, members, media" >/dev/null 2>&1

echo "Part A: the contract"
start_app
U="http://127.0.0.1:$PORT"
health="$(http_body GET "$U/health")"
expect "GET /health reports status ok" '"status":"ok"' "$health"
expect "GET /health reports version 2.2.0" '"version":"2.2.0"' "$health"
expect "GET /health reports storage postgres" '"storage":"postgres"' "$health"
expect "GET /health counts books" '"books":0' "$health"
check "the app created the tables (media exists)" sh -c "[ \"\$(PGPASSWORD='$DB_PASSWORD' psql -h '$PG_HOST' -p '$PG_PORT' -U '$DB_USER' -d '$PG_DB' -t -A -c \"SELECT count(*) FROM media\")\" = '0' ]"

code="$(http_code POST "$U/books" -H 'Content-Type: application/json' -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884"}')"
[ "$code" = "201" ] && ok "POST /books answers 201" || fail "POST /books answered $code, expected 201"
http_code POST "$U/books" -H 'Content-Type: application/json' -d '{"title":"The Pragmatic Programmer","author":"David Thomas","isbn":"9780135957059","kind":"AUDIOBOOK","durationMinutes":720}' >/dev/null
location="$(curl -s -i --max-time 5 -X POST "$U/books" -H 'Content-Type: application/json' -d '{"title":"Effective Java","author":"Joshua Bloch","isbn":"9780134685991"}' | tr -d '\r' | sed -n 's/^[Ll]ocation: //p')"
[ "$location" = "/books/3" ] && ok "POST /books sets Location: /books/3" || fail "POST /books Location is '$location', expected /books/3"
code="$(http_code POST "$U/books" -H 'Content-Type: application/json' -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884"}')"
[ "$code" = "409" ] && ok "POST /books with a known ISBN answers 409" || fail "duplicate ISBN answered $code, expected 409"
code="$(http_code POST "$U/books" -H 'Content-Type: application/json' -d '{"author":"nobody","isbn":"1"}')"
[ "$code" = "400" ] && ok "POST /books without a title answers 400" || fail "missing title answered $code, expected 400"
body="$(http_body POST "$U/books" -H 'Content-Type: application/json' -d '{"author":"nobody","isbn":"1"}')"
expect "errors come as {\"error\": ...}" '"error"' "$body"
code="$(http_code POST "$U/books" -H 'Content-Type: application/json' -d '{"title":"x","author":"y","isbn":"2","kind":"VINYL"}')"
[ "$code" = "400" ] && ok "POST /books with an unknown kind answers 400" || fail "unknown kind answered $code, expected 400"

psql_cmd "INSERT INTO members (name) VALUES ('Ada Lovelace')" >/dev/null
code="$(http_code POST "$U/loans" -H 'Content-Type: application/json' -d '{"bookId":1,"memberId":1}')"
[ "$code" = "201" ] && ok "POST /loans answers 201" || fail "POST /loans answered $code, expected 201"
loan="$(http_body POST "$U/loans" -H 'Content-Type: application/json' -d '{"bookId":2,"memberId":1}')"
expect "POST /loans returns the loan with bookId" '"bookId":2' "$loan"
expect "POST /loans returns status OPEN" '"status":"OPEN"' "$loan"
expect "POST /loans returns a due date" '"due":"' "$loan"
code="$(http_code POST "$U/loans" -H 'Content-Type: application/json' -d '{"bookId":1,"memberId":1}')"
[ "$code" = "409" ] && ok "borrowing a borrowed medium answers 409" || fail "second loan answered $code, expected 409"
code="$(http_code POST "$U/loans" -H 'Content-Type: application/json' -d '{"bookId":99,"memberId":1}')"
[ "$code" = "404" ] && ok "unknown medium answers 404" || fail "unknown medium answered $code, expected 404"
code="$(http_code POST "$U/loans" -H 'Content-Type: application/json' -d '{"bookId":1,"memberId":99}')"
[ "$code" = "404" ] && ok "unknown member answers 404" || fail "unknown member answered $code, expected 404"

books="$(http_body GET "$U/books?available=true")"
expect "GET /books?available=true lists the free medium" '"id":3' "$books"
expect_not "GET /books?available=true leaves out borrowed media" '"id":1,' "$books"
code="$(http_code GET "$U/books?available=maybe")"
[ "$code" = "400" ] && ok "GET /books?available=maybe answers 400" || fail "available=maybe answered $code, expected 400"

returned="$(http_body PATCH "$U/loans/1" -H 'Content-Type: application/json' -d '{"returned":true}')"
expect "PATCH /loans/1 returns status RETURNED" '"status":"RETURNED"' "$returned"
expect "PATCH /loans/1 returns daysOverdue" '"daysOverdue":' "$returned"
expect "PATCH /loans/1 returns lateFee" '"lateFee":' "$returned"
code="$(http_code PATCH "$U/loans/1" -H 'Content-Type: application/json' -d '{"returned":true}')"
[ "$code" = "409" ] && ok "returning twice answers 409" || fail "second return answered $code, expected 409"
code="$(http_code PATCH "$U/loans/99" -H 'Content-Type: application/json' -d '{"returned":true}')"
[ "$code" = "404" ] && ok "unknown loan answers 404" || fail "unknown loan answered $code, expected 404"
code="$(http_code PATCH "$U/loans/2" -H 'Content-Type: application/json' -d '{"returned":false}')"
[ "$code" = "400" ] && ok "PATCH with returned:false answers 400" || fail "returned:false answered $code, expected 400"

code="$(http_code GET "$U/nothing")"
[ "$code" = "404" ] && ok "unknown path answers 404" || fail "unknown path answered $code, expected 404"
allow="$(curl -s -i --max-time 5 -X DELETE "$U/books" | tr -d '\r' | sed -n 's/^[Aa]llow: //p')"
[ -n "$allow" ] && ok "wrong method answers with an Allow header ($allow)" || fail "DELETE /books has no Allow header"
code="$(http_code DELETE "$U/books")"
[ "$code" = "405" ] && ok "wrong method answers 405" || fail "DELETE /books answered $code, expected 405"

health="$(http_body GET "$U/health")"
expect "GET /health counts 3 books" '"books":3' "$health"
expect "GET /health counts 1 open loan" '"openLoans":1' "$health"
if grep -q '"method":"POST","path":"/loans","status":201' "$WORK/app.log"; then
  ok "LOG_FORMAT=json writes one JSON line per request with method, path and status"
else
  fail "no JSON log line with method, path and status for POST /loans in the app output"
fi

echo "Part A: persistence"
stop_app
start_app
health="$(http_body GET "$U/health")"
expect "after a restart /health still counts 3 books" '"books":3' "$health"
expect "after a restart the open loan is still there" '"openLoans":1' "$health"
books="$(http_body GET "$U/books?available=false")"
expect "after a restart medium 2 is still borrowed" '"id":2' "$books"
stop_app

echo "Part A: without DATABASE_URL"
(cd "$WORK" && PORT="$PORT" exec java -cp "$CP" shelfy.Main serve >"$WORK/file.log" 2>&1) &
APP_PID=$!
if wait_for_port "$PORT" 20; then
  health="$(http_body GET "$U/health")"
  expect "without DATABASE_URL the storage is the text file" '"storage":"file"' "$health"
  http_code POST "$U/books" -H 'Content-Type: application/json' -d '{"title":"Refactoring","author":"Martin Fowler","isbn":"9780134757599"}' >/dev/null
  check "shelfy.txt is written in the working directory" test -f "$WORK/shelfy.txt"
else
  fail "the app did not start without DATABASE_URL"
fi
stop_app

summary
