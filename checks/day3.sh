#!/usr/bin/env bash
# Day 3, Module 11: acceptance checks for "shelfy 2.1 as a tested Maven project".
# Expects: your Maven project in day3/shelfy (sources in src/main/java/shelfy, tests in src/test/java/shelfy).
# Run from the repository root:  ./checks/day3.sh
# The script runs ./mvnw -q verify in your project, then the one-shot commands in an empty working directory.
set -u
. "$(dirname "$0")/lib.sh"
LAB="$(cd "$(dirname "$0")/.." && pwd)"
need_java

PROJECT="${SHELFY_PROJECT:-$LAB/day3/shelfy}"
BUILD="${SHELFY_BUILD:-./mvnw -q verify}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "Part A: project and tests"
check "pom.xml exists" test -f "$PROJECT/pom.xml"
check "mvnw (Maven Wrapper) exists and is executable" test -x "$PROJECT/mvnw"
check "sources are in src/main/java/shelfy" test -f "$PROJECT/src/main/java/shelfy/Library.java"
check "tests are in src/test/java/shelfy" test -f "$PROJECT/src/test/java/shelfy/LibraryTest.java"
if (cd "$PROJECT" && $BUILD >"$WORK/build.log" 2>&1); then
  ok "$BUILD ends with BUILD SUCCESS"
else
  fail "$BUILD failed:"
  tail -n 30 "$WORK/build.log"
  summary
fi
tests=0
for report in "$PROJECT"/target/surefire-reports/TEST-shelfy.LibraryTest.xml; do
  if [ -f "$report" ]; then
    tests="$(sed -n 's/.*<testsuite[^>]* tests="\([0-9]*\)".*/\1/p' "$report" | head -n 1)"
  fi
done
if [ "${tests:-0}" -ge 8 ]; then
  ok "LibraryTest ran $tests tests (at least 8)"
else
  fail "LibraryTest ran ${tests:-0} tests, expected at least 8 (counted from target/surefire-reports)"
fi
check "a @ParameterizedTest covers the late fee" grep -q '@ParameterizedTest' "$PROJECT/src/test/java/shelfy/LibraryTest.java"
if grep -v '^\s*//' "$PROJECT/src/main/java/shelfy/Library.java" | grep -q 'LocalDate.now()'; then
  fail "Library.java calls LocalDate.now(); pass the date as a parameter instead (Module 10.8)"
else
  ok "Library.java does not call LocalDate.now()"
fi
check "Library.findMember returns an Optional" sh -c "javap -cp '$PROJECT/target/classes' shelfy.Library | grep -q 'java.util.Optional<shelfy.Member> findMember(int)'"

echo "Part B: queries"
RUN="$WORK/run"
mkdir -p "$RUN"
cd "$RUN" || exit 1
shelfy() {
  java -cp "$PROJECT/target/classes" shelfy.Main "$@" 2>&1
}
shelfy add "Clean Code" "Robert C. Martin" 9780132350884 >/dev/null
shelfy add "The Pragmatic Programmer" "David Thomas" 9780135957059 audiobook 720 >/dev/null
shelfy add "Effective Java" "Joshua Bloch" 9780134685991 >/dev/null
shelfy member Ada Lovelace >/dev/null
shelfy member Grace Hopper >/dev/null
shelfy borrow 1 1 2026-01-01 >/dev/null
shelfy borrow 2 2 2026-01-01 >/dev/null

out="$(shelfy available)"
expect "available lists the medium on the shelf" "Effective Java" "$out"
expect_not "available leaves out borrowed media" "Clean Code" "$out"
expect "available ends with the count" "1 available" "$out"

out="$(shelfy overdue 2026-01-21)"
expect "overdue lists the book loan" "Clean Code" "$out"
expect "overdue lists the audiobook loan" "Pragmatic" "$out"
expect "overdue ends with the count and the date" "2 overdue on 2026-01-21" "$out"
out="$(shelfy overdue 2026-01-10)"
expect "nothing is overdue before the due date" "0 overdue on 2026-01-10" "$out"

out="$(shelfy stats)"
expect "stats prints the number of media" "media: 3" "$out"
expect "stats prints the open loans" "open loans: 2" "$out"
expect "stats prints the open loans of Ada" "Ada Lovelace: 1" "$out"
expect "stats prints the open loans of Grace" "Grace Hopper: 1" "$out"

out="$(shelfy search MARTIN)"
expect "search finds the author, ignoring case" "Clean Code" "$out"
expect "search ends with the count" '1 found for "MARTIN"' "$out"
out="$(shelfy search java)"
expect "search finds in the title" "Effective Java" "$out"
expect_not "search does not list media that do not match" "Clean Code" "$out"

out="$(shelfy fees 2026-01-21)"
expect "fees still works: book 1.50" "fee 1.50" "$out"
expect "fees still works: audiobook 3.00" "fee 3.00" "$out"
content="$(cat shelfy.txt 2>/dev/null)"
expect "shelfy.txt keeps the Day 2 format (MEDIUM line)" "MEDIUM;2;AUDIOBOOK;The Pragmatic Programmer;David Thomas;9780135957059;720" "$content"
expect "shelfy.txt keeps the Day 2 format (LOAN line)" "LOAN;1;1;1;2026-01-01;2026-01-15;" "$content"

summary
