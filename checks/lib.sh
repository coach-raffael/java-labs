#!/usr/bin/env bash
# Shared helpers for the acceptance checks day1.sh to day5.sh. Not meant to be run directly.
# Every check prints "ok" or "FAIL" with a short reason; the script ends with PASS or FAIL.

FAILED=0
PASSED=0

ok() {
  PASSED=$((PASSED + 1))
  printf '  ok    %s\n' "$1"
}

fail() {
  FAILED=$((FAILED + 1))
  printf '  FAIL  %s\n' "$1"
}

# check "description" command...   -> ok if the command succeeds (output hidden)
check() {
  local description="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    ok "$description"
  else
    fail "$description"
  fi
}

# contains STRING TEXT  -> true if TEXT contains STRING
contains() {
  case "$2" in *"$1"*) return 0 ;; esac
  return 1
}

# expect "description" STRING TEXT -> ok if TEXT contains STRING
expect() {
  if contains "$2" "$3"; then
    ok "$1"
  else
    fail "$1 (expected to see: $2)"
  fi
}

# expect_not "description" STRING TEXT -> ok if TEXT does not contain STRING
expect_not() {
  if contains "$2" "$3"; then
    fail "$1 (must not see: $2)"
  else
    ok "$1"
  fi
}

# need_java -> fails the whole script when java is not version 25 or newer or a JDK tool is missing
need_java() {
  for tool in java javac javap curl; do
    if ! command -v "$tool" >/dev/null 2>&1; then
      echo "$tool is not on your PATH. Module 00 shows how to install JDK 25 (not a JRE) and curl."
      exit 1
    fi
  done
  local major
  major="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1)"
  if [ -z "$major" ] || [ "$major" -lt 25 ]; then
    echo "java reports version ${major:-unknown}; the course needs JDK 25 or newer. Module 00 shows how to install it."
    exit 1
  fi
}

# http_code METHOD URL [curl args...] -> prints only the HTTP status code (000 when unreachable)
http_code() {
  local method="$1"
  local url="$2"
  shift 2
  curl -s -o /dev/null -w '%{http_code}' --max-time 5 -X "$method" "$url" "$@"
}

# http_body METHOD URL [curl args...] -> prints the response body (empty when unreachable)
http_body() {
  local method="$1"
  local url="$2"
  shift 2
  curl -s --max-time 5 -X "$method" "$url" "$@"
}

# json_has KEY VALUE JSON -> true if the JSON text contains "KEY":VALUE or "KEY":"VALUE" (spaces allowed)
json_has() {
  printf '%s' "$3" | grep -Eq "\"$1\"[[:space:]]*:[[:space:]]*\"?$2\"?"
}

# wait_for_port PORT [seconds] -> true when something answers on 127.0.0.1:PORT in time
wait_for_port() {
  local port="$1"
  local seconds="${2:-20}"
  local i=0
  while [ "$i" -lt "$((seconds * 2))" ]; do
    if curl -s -o /dev/null --max-time 1 "http://127.0.0.1:$port/health"; then
      return 0
    fi
    sleep 0.5
    i=$((i + 1))
  done
  return 1
}

summary() {
  echo
  if [ "$FAILED" -eq 0 ]; then
    echo "PASS ($PASSED checks)"
    exit 0
  fi
  echo "FAIL ($FAILED of $((PASSED + FAILED)) checks failed)"
  exit 1
}
