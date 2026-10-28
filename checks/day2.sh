#!/usr/bin/env bash
# Day 2, Module 07: acceptance checks for "shelfy 2.0 remembers its books".
# Expects: your sources in day2/shelfy/src/shelfy/*.java (package shelfy, class shelfy.Main).
# Run from the repository root:  ./checks/day2.sh
# The script compiles into a temporary directory and runs the program in an empty working directory,
# so your own shelfy.txt is not touched.
set -u
. "$(dirname "$0")/lib.sh"
LAB="$(cd "$(dirname "$0")/.." && pwd)"
need_java

SRC="${SHELFY_SRC:-$LAB/day2/shelfy/src/shelfy}"
WORK="$(mktemp -d)"
OUT="$WORK/out"
trap 'rm -rf "$WORK"' EXIT

echo "Part A: the model"
if javac -d "$OUT" "$SRC"/*.java 2>"$WORK/compile.log"; then
  ok "javac -d out src/shelfy/*.java compiles without errors"
else
  fail "the sources do not compile:"
  head -n 20 "$WORK/compile.log"
  summary
fi
for cls in Medium Book Audiobook Member Loan MediaKind LoanStatus Storage InMemoryStorage FileStorage Library \
           BookNotAvailableException NoSuchMemberException; do
  check "class shelfy.$cls exists" test -f "$OUT/shelfy/$cls.class"
done
check "Medium is abstract" sh -c "javap -cp '$OUT' shelfy.Medium | grep -q 'abstract class shelfy.Medium'"
check "Book extends Medium" sh -c "javap -cp '$OUT' shelfy.Book | grep -q 'extends shelfy.Medium'"
check "Audiobook extends Medium" sh -c "javap -cp '$OUT' shelfy.Audiobook | grep -q 'extends shelfy.Medium'"
check "Loan is a record" sh -c "javap -cp '$OUT' shelfy.Loan | grep -q 'extends java.lang.Record'"
check "MediaKind is an enum" sh -c "javap -cp '$OUT' shelfy.MediaKind | grep -q 'extends java.lang.Enum'"
check "LoanStatus is an interface" sh -c "javap -cp '$OUT' shelfy.LoanStatus | grep -q 'interface shelfy.LoanStatus'"
check "LoanStatus is sealed (javap -v lists PermittedSubclasses)" sh -c "javap -v -cp '$OUT' shelfy.LoanStatus | grep -q '^PermittedSubclasses'"
check "InMemoryStorage implements Storage" sh -c "javap -cp '$OUT' shelfy.InMemoryStorage | grep -q 'implements shelfy.Storage'"
check "FileStorage implements Storage" sh -c "javap -cp '$OUT' shelfy.FileStorage | grep -q 'implements shelfy.Storage'"
check "Library has the constant LOAN_DAYS" sh -c "javap -p -cp '$OUT' shelfy.Library | grep -q 'static final int LOAN_DAYS'"

echo "Part B: acceptance"
RUN="$WORK/run"
mkdir -p "$RUN"
cd "$RUN" || exit 1
shelfy() {
  java -cp "$OUT" shelfy.Main "$@" 2>&1
}

out="$(shelfy list)"
status=$?
if [ "$status" -eq 0 ] && ! contains "Exception" "$out"; then
  ok "first start without shelfy.txt: empty library, no exception"
else
  fail "first start without shelfy.txt failed (exit $status): $(printf '%s' "$out" | head -n 2)"
fi
expect "list on an empty library prints 0 media" "0 media" "$out"

out="$(printf 'add\nClean Code\nRobert C. Martin\n9780132350884\n\nadd\nThe Pragmatic Programmer\nDavid Thomas\n9780135957059\naudiobook\n720\nmember Ada Lovelace\nlist\nquit\n' | shelfy)"
expect "add in the console asks for the title" "Title:" "$out"
expect "add in the console asks for the kind" "Kind" "$out"
expect "list shows the book as (Book) [available]" "1. Clean Code by Robert C. Martin (Book) [available]" "$out"
expect "list shows the audiobook as (Audiobook)" "2. The Pragmatic Programmer by David Thomas (Audiobook)" "$out"
expect "list counts 2 media, 0 borrowed" "2 media, 0 borrowed" "$out"
expect "list shows the member" "Ada Lovelace" "$out"

if [ -f shelfy.txt ]; then
  ok "shelfy.txt exists after add"
else
  fail "shelfy.txt was not written in the working directory"
fi
content="$(cat shelfy.txt 2>/dev/null)"
expect "shelfy.txt has the MEDIUM line of the book" "MEDIUM;1;BOOK;Clean Code;Robert C. Martin;9780132350884;0" "$content"
expect "shelfy.txt has the MEDIUM line of the audiobook with 720 minutes" "MEDIUM;2;AUDIOBOOK;The Pragmatic Programmer;David Thomas;9780135957059;720" "$content"
expect "shelfy.txt has the MEMBER line" "MEMBER;1;Ada Lovelace" "$content"

out="$(shelfy borrow 1 1 2026-01-01; shelfy borrow 2 1 2026-01-01)"
expect "borrow 1 1 2026-01-01 prints Borrowed" "Borrowed" "$out"
expect "the loan is due 14 days later" "2026-01-15" "$out"
content="$(cat shelfy.txt 2>/dev/null)"
expect "shelfy.txt has the open LOAN line" "LOAN;1;1;1;2026-01-01;2026-01-15;" "$content"

out="$(shelfy list)"
expect "after a restart the book is still [borrowed] (persistence)" "1. Clean Code by Robert C. Martin (Book) [borrowed]" "$out"
expect "after a restart list counts 2 borrowed" "2 media, 2 borrowed" "$out"

out="$(printf 'borrow 1 1\nborrow 9 1\nborrow 1 9\nreturn 1 2026-01-21\nlist\nquit\n' | shelfy)"
status=$?
if [ "$status" -eq 0 ] && ! contains "Exception" "$out"; then
  ok "wrong input prints a message and the program goes on (no stack trace, exit 0)"
else
  fail "wrong input ended the program or printed a stack trace (exit $status)"
fi
expect "borrowing a borrowed medium prints Already borrowed" "Already borrowed" "$out"
expect "unknown medium number prints No such medium" "No such medium" "$out"
expect "unknown member number prints No such member" "No such member" "$out"
expect "return 1 2026-01-21 prints Returned" "Returned" "$out"
expect "after the return the book is [available] again" "1. Clean Code by Robert C. Martin (Book) [available]" "$out"

out="$(shelfy fees 2026-01-21)"
expect "fees shows the audiobook loan as open with a fee of 3.00 (6 days overdue, 3 free)" "fee 3.00" "$out"
expect_not "the returned book has no open loan any more" "Clean Code" "$out"
out="$(shelfy borrow 1 1 2026-01-01; shelfy fees 2026-01-21)"
expect "a book overdue by the same 6 days costs 1.50 (polymorphism)" "fee 1.50" "$out"
out="$(shelfy fees 2026-01-10)"
expect_not "no fee before the due date" "fee 1.50" "$out"

summary
