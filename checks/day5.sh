#!/usr/bin/env bash
# Day 5, Module 18: acceptance checks for "Ship shelfy 2.3 and fix the broken one".
# Part A expects day5/shelfy built (./mvnw verify, ./package.sh) and either the installed package
# (/opt/shelfy/bin/shelfy) or the app image (day5/shelfy/target/pkg/shelfy/bin/shelfy).
# Part B expects your repaired project in day5/shelfy-broken with FIXES.md.
# Run from the repository root:  ./checks/day5.sh
set -u
. "$(dirname "$0")/lib.sh"
LAB="$(cd "$(dirname "$0")/.." && pwd)"
need_java

SHIP="${SHELFY_SHIP:-$LAB/day5/shelfy}"
BROKEN="${SHELFY_BROKEN:-$LAB/day5/shelfy-broken}"
BUILD="${SHELFY_BUILD:-./mvnw -q verify}"
PORT="${SHELFY_CHECK_PORT:-18089}"
WORK="$(mktemp -d)"
APP_PID=""
cleanup() {
  if [ -n "$APP_PID" ]; then
    kill "$APP_PID" 2>/dev/null
  fi
  rm -rf "$WORK"
}
trap cleanup EXIT

serve_and_check() {
  # serve_and_check "description" command...  -> starts the command in the background, expects version 2.3.0 on /health
  local description="$1"
  shift
  (cd "$WORK" && exec "$@" >"$WORK/serve.log" 2>&1) &
  APP_PID=$!
  if wait_for_port "$PORT" 20; then
    local health
    health="$(http_body GET "http://127.0.0.1:$PORT/health")"
    expect "$description: /health reports version 2.3.0" '"version":"2.3.0"' "$health"
  else
    fail "$description: nothing answered on port $PORT"
    tail -n 5 "$WORK/serve.log"
  fi
  kill "$APP_PID" 2>/dev/null
  wait "$APP_PID" 2>/dev/null
  APP_PID=""
}

echo "Part A: the executable JAR"
JAR="$SHIP/target/dist/shelfy-2.3.0.jar"
if [ -f "$JAR" ]; then
  ok "target/dist/shelfy-2.3.0.jar exists"
else
  fail "target/dist/shelfy-2.3.0.jar is missing (Module 17.1: maven-jar-plugin with outputDirectory target/dist)"
fi
if command -v unzip >/dev/null 2>&1; then
  manifest="$(unzip -p "$JAR" META-INF/MANIFEST.MF 2>/dev/null | tr -d '\r')"
else
  manifest="$(cd "$WORK" && jar xf "$JAR" META-INF/MANIFEST.MF 2>/dev/null && tr -d '\r' < META-INF/MANIFEST.MF)"
fi
expect "the manifest names the main class" "Main-Class: shelfy.Main" "$manifest"
expect "the manifest lists the dependencies under lib/" "lib/" "$manifest"
check "target/dist/lib holds the dependencies (jackson-databind)" sh -c "ls '$SHIP'/target/dist/lib/jackson-databind-*.jar"
check "target/dist/lib holds the dependencies (postgresql)" sh -c "ls '$SHIP'/target/dist/lib/postgresql-*.jar"
if [ -f "$JAR" ]; then
  serve_and_check "java -jar" env PORT="$PORT" java -jar "$JAR" --storage memory serve
fi

echo "Part A: runtime and package"
LAUNCHER=""
for candidate in /opt/shelfy/bin/shelfy "$SHIP/target/pkg/shelfy/bin/shelfy" "$SHIP/target/pkg/shelfy.app/Contents/MacOS/shelfy"; do
  if [ -x "$candidate" ]; then
    LAUNCHER="$candidate"
    break
  fi
done
if [ -n "$LAUNCHER" ]; then
  ok "launcher found: $LAUNCHER"
  # PATH without any java: the launcher must bring its own runtime
  serve_and_check "launcher without java on the PATH" env -i PATH=/nonexistent HOME="$WORK" PORT="$PORT" "$LAUNCHER" --storage memory serve
  RELEASE="$(dirname "$LAUNCHER")/../lib/runtime/release"
  [ -f "$RELEASE" ] || RELEASE="$(dirname "$LAUNCHER")/../runtime/Contents/Home/release"
  if [ -f "$RELEASE" ]; then
    modules="$(sed -n 's/^MODULES="\(.*\)"/\1/p' "$RELEASE")"
    expect "the runtime contains jdk.httpserver" "jdk.httpserver" "$modules"
    expect "the runtime contains java.sql" "java.sql" "$modules"
    expect_not "the runtime is trimmed (no jdk.compiler)" "jdk.compiler" "$modules"
    expect_not "the runtime is trimmed (no jdk.jshell)" "jdk.jshell" "$modules"
    count="$(printf '%s' "$modules" | wc -w)"
    ok "the runtime has $count modules (a full JDK has about 70)"
  else
    fail "no release file next to the launcher (expected lib/runtime/release)"
  fi
else
  fail "no launcher: install the package or build the app image into day5/shelfy/target/pkg/shelfy"
fi
packages="$(ls "$SHIP"/target/pkg/*.rpm "$SHIP"/target/pkg/*.deb "$SHIP"/target/pkg/*.dmg "$SHIP"/target/pkg/*.pkg "$SHIP"/target/pkg/*.msi 2>/dev/null)"
if [ -n "$packages" ]; then
  ok "native package built: $(basename "$packages" | tr '\n' ' ')"
elif [ -x /opt/shelfy/bin/shelfy ]; then
  ok "the package is installed under /opt/shelfy"
elif [ -n "$LAUNCHER" ]; then
  ok "no rpm, deb, dmg, pkg or msi in target/pkg; the app image counts on Codespaces and macOS"
else
  fail "no package and no app image in day5/shelfy/target/pkg"
fi

if [ "${SHELFY_SKIP_PART_B:-}" = "1" ]; then
  echo "Part B skipped (SHELFY_SKIP_PART_B=1, used by the course factory's CI)"
  summary
fi

echo "Part B: shelfy-broken repaired"
if (cd "$BROKEN" && $BUILD >"$WORK/broken-build.log" 2>&1); then
  ok "$BUILD is green in day5/shelfy-broken (BrokenTest passes)"
else
  fail "$BUILD is red in day5/shelfy-broken:"
  grep -E "Tests run|FAIL|ERROR" "$WORK/broken-build.log" | head -n 10
fi
if [ -f "$BROKEN/FIXES.md" ]; then
  lines="$(grep -c '[A-Za-z]' "$BROKEN/FIXES.md")"
  if [ "$lines" -ge 8 ]; then
    ok "FIXES.md has $lines lines of text (at least 8)"
  else
    fail "FIXES.md has only $lines lines of text, one per fix makes 8"
  fi
else
  fail "FIXES.md is missing in day5/shelfy-broken"
fi
SRC="$BROKEN/src/main/java/shelfy"
if grep -nE '[!=]= *"' "$SRC"/*.java >/dev/null; then
  fail "a string is still compared with == or != (grep -nE '[!=]= *\"' src/main/java/shelfy/*.java)"
else
  ok "no string is compared with == any more"
fi
check "Member overrides hashCode" grep -q 'public int hashCode()' "$SRC/Member.java"
if grep -q 'static int openLoans' "$SRC/Library.java"; then
  fail "Library still has the static counter openLoans"
else
  ok "Library has no static counter any more"
fi
if grep -qE '^\s*List [a-z]' "$SRC/Library.java"; then
  fail "Library still uses the raw type List"
else
  ok "Library uses no raw type List"
fi
if grep -q 'try (Connection' "$SRC/PostgresStorage.java" && ! grep -q 'connections.add' "$SRC/PostgresStorage.java"; then
  ok "PostgresStorage opens every connection in try-with-resources and keeps none"
else
  fail "PostgresStorage still keeps connections open (connections.add) or does not use try-with-resources"
fi
# every catch (SQLException ...) must be followed by a throw (no empty catch, no silent return)
catches="$(grep -c 'catch (SQLException' "$SRC/PostgresStorage.java")"
throwing="$(awk '/catch \(SQLException/ {want=1; next} want && NF {if ($0 ~ /throw /) n++; want=0} END {print n+0}' "$SRC/PostgresStorage.java")"
if [ "$catches" -gt 0 ] && [ "$catches" -eq "$throwing" ]; then
  ok "every SQLException in PostgresStorage is turned into a StorageException ($catches catch blocks)"
else
  fail "PostgresStorage still swallows an SQLException ($throwing of $catches catch blocks throw)"
fi
if grep -qE 'return null;' "$SRC/Library.java"; then
  fail "Library still returns null (findMember); return an Optional instead"
else
  ok "Library returns no null for a missing member"
fi
if grep -q 'plusDays(LOAN_DAYS)' "$SRC/Library.java"; then
  fail "Library.borrow still uses the constant LOAN_DAYS instead of the configured loanDays"
else
  ok "Library.borrow uses the configured loan period"
fi

summary
