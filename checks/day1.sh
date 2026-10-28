#!/usr/bin/env bash
# Day 1, Module 03: acceptance checks for "shelfy 1.0, a console library".
# Expects: your program in day1/shelfy/Shelfy.java (run with: java Shelfy.java).
# Run from the repository root:  ./checks/day1.sh
# The script feeds a command sequence through a pipe and looks for the expected lines.
set -u
. "$(dirname "$0")/lib.sh"
LAB="$(cd "$(dirname "$0")/.." && pwd)"
need_java

SRC="$LAB/day1/shelfy/Shelfy.java"
if [ ! -f "$SRC" ]; then
  echo "day1/shelfy/Shelfy.java not found"
  exit 1
fi

echo "Part A: the library"
out="$(printf 'list\nquit\n' | java "$SRC" 2>&1)"
status=$?
if [ "$status" -eq 0 ]; then
  ok "java Shelfy.java runs and exits with 0 after quit"
else
  fail "java Shelfy.java exited with $status (compile error or crash?)"
  printf '%s\n' "$out" | head -n 20
fi
expect "prints shelfy 1.0 at the start" "shelfy 1.0" "$out"
expect "list shows book 1 as available" "1. " "$out"
expect "list shows book 3 as available" "3. " "$out"
expect "list marks books as [available]" "[available]" "$out"

out="$(printf 'add\nRefactoring\nMartin Fowler\nlist\nquit\n' | java "$SRC" 2>&1)"
expect "add asks for the title" "Title:" "$out"
expect "add asks for the author" "Author:" "$out"
expect "the new book appears in the list as number 4" "4. Refactoring by Martin Fowler [available]" "$out"

out="$(printf 'borrow 2\nlist\nreturn 2\nlist\nquit\n' | java "$SRC" 2>&1)"
expect "borrow 2 prints Borrowed:" "Borrowed:" "$out"
expect "list shows book 2 as [borrowed]" "2. The Pragmatic Programmer by David Thomas [borrowed]" "$out"
expect "return 2 prints Returned:" "Returned:" "$out"
expect "after return, list shows book 2 as [available] again" "2. The Pragmatic Programmer by David Thomas [available]" "$out"

echo "Part B: robust input"
out="$(printf 'xyz\nborrow 2\nborrow 2\nborrow 9\nborrow 0\nborrow abc\nborrow\nreturn 1\nlist\nquit\n' | java "$SRC" 2>&1)"
status=$?
if [ "$status" -eq 0 ]; then
  ok "wrong input never ends the program (exit code 0)"
else
  fail "the program ended with exit code $status on wrong input"
fi
expect_not "no exception reaches the user" "Exception" "$out"
expect "unknown command prints Unknown command: xyz" "Unknown command: xyz" "$out"
n="$(printf '%s\n' "$out" | grep -c 'borrow <nr>')"
if [ "$n" -ge 2 ]; then
  ok "the command list is shown again after an unknown command"
else
  fail "the command list should appear at the start and again after the unknown command (seen $n times)"
fi
expect "borrowing a borrowed book prints Already borrowed" "Already borrowed" "$out"
n="$(printf '%s\n' "$out" | grep -c 'No such book')"
if [ "$n" -ge 4 ]; then
  ok "borrow 9, borrow 0, borrow abc and borrow without a number print No such book"
else
  fail "expected No such book four times (number too high, zero, not a number, missing), seen $n"
fi
expect "the loop still works afterwards (list prints 3 books)" "3 books" "$out"

summary
