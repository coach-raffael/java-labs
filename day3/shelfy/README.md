# Lab Module 11: shelfy 2.1 as a tested Maven project

Starter for the lab lesson of Day 3: the finished shelfy 2.0 (`src/shelfy/`, the same code as
`shelfy/v2.0`) plus a ready `pom.xml` and the Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`).
Build shelfy 2.1 from it. Check your work from the repository root with `./checks/day3.sh`.

## Acceptance criteria

### Part A, project and tests

1. Standard Maven layout: sources in `src/main/java/shelfy/`, tests in `src/test/java/shelfy/`.
   `./mvnw verify` compiles both and runs the tests; it ends with `BUILD SUCCESS`.
2. JUnit 6 tests in `LibraryTest`, at least eight: borrow, give back, borrowing twice throws,
   late fee per kind of medium, due date `LOAN_DAYS` days after the borrow date, overdue at a
   fixed date, search, stats. Use `@ParameterizedTest` for the late fee.
3. No `LocalDate.now()` in `Library`: the date is always a parameter (Module 10.8).

### Part B, queries

4. New commands, each one a stream pipeline in `Library`:
   `available` (media on the shelf), `overdue <yyyy-mm-dd>` (open loans past their due date on
   that day), `stats` (media, open loans, overdue loans, open loans per member),
   `search <text>` (finds in title and author, ignoring case, sorted by title).
5. `Library.findMember(int)` returns an `Optional<Member>`.
6. Everything from Day 2 still works: the console, the one-shot commands, `shelfy.txt` keeps its
   format, `./checks/day2.sh` with `SHELFY_SRC=day3/shelfy/src/main/java/shelfy` still passes.
7. Output: `available` lists the media as `list` does (without the bracket) and ends with `<n> available`;
   `overdue` prints one line per loan with the title and ends with `<n> overdue on <date>`;
   `search` lists the matches and ends with `<n> found for "<text>"`; `stats` prints `media: <n>`,
   `open loans: <n>`, `overdue loans: <n>` and one indented line `  <member name>: <open loans>` per member.
