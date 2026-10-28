# Lab Module 07: shelfy 2.0 remembers its books

Starter for the lab lesson of Day 2. Build shelfy 2.0 here from what Modules 04 to 06 taught.

```
javac -d out src/shelfy/*.java
java -cp out shelfy.Main
```

Two files are given: `src/shelfy/Main.java` (the menu loop, compiles on its own) and
`src/shelfy/Storage.java` (the interface your storages implement; it compiles once `Medium`,
`Member` and `Loan` exist, so write those first). `shelfy/v1.0/Shelfy.java` in this repository
shows the Day 1 program for comparison. Check your work from the repository root with
`./checks/day2.sh`.

## Acceptance criteria

### Part A, the model

1. Package `shelfy`. One class per file.
2. `Medium` (abstract) with `Book` and `Audiobook`; an audiobook has a duration in minutes.
   Each kind answers `lateFeePerDay()` itself: 0.50 for a book, 1.00 for an audiobook.
3. `Member` (class) with `equals` and `hashCode` on the member number.
4. `Loan` (record) with `borrowedOn`, `due`, `returnedOn` (null while open); the compact
   constructor rejects null dates, ids below 1 and a due date before the borrow date.
5. `MediaKind` (enum) with `BOOK` and `AUDIOBOOK`.
6. `LoanStatus` (sealed interface) with `Open`, `Returned` and `Overdue(long daysOverdue)`,
   computed from the loan and a date you pass in.
7. `Storage` (interface, given) with `InMemoryStorage` and `FileStorage`.
8. `Library` with `borrow`, `giveBack`, `lateFee` and the constant `LOAN_DAYS = 14`.
   The first 3 days overdue are free, then each day costs what the medium charges, at most 10.00.
9. Your own exceptions `BookNotAvailableException` and `NoSuchMemberException`
   (a `NoSuchMediumException` for an unknown medium number is a good third one).

### Part B, acceptance

10. Commands in the console: `add` (asks `Title: `, `Author: `, `ISBN: `, `Kind (book or audiobook,
    default book): ` and for an audiobook `Minutes: `), `list`, `member <name>`,
    `borrow <mediumNr> <memberNr> [yyyy-mm-dd]`, `return <mediumNr> [yyyy-mm-dd]`,
    `fees <yyyy-mm-dd>`, `help`, `quit`. The optional date lets the check script test fees
    without waiting two weeks; without it, today counts. `borrow` prints `Borrowed: <title>, due <yyyy-mm-dd>`
    (the due date is the borrow date plus `LOAN_DAYS`), `return` prints `Returned: <title>, <n> days overdue, fee <amount>`.
11. The same commands work as arguments for one-shot calls, for example
    `java -cp out shelfy.Main add "Clean Code" "Robert C. Martin" 9780132350884` and
    `java -cp out shelfy.Main list`. `--storage memory` or `--storage file` (default) may come first.
12. `list` prints one line per medium: `<id>. <title> by <author> (<Kind>) [available]` or
    `[borrowed]`, then `<n> media, <m> borrowed`, then the members.
13. `shelfy.txt` in the working directory holds one line per medium, member and loan:
    `MEDIUM;1;BOOK;Clean Code;Robert C. Martin;9780132350884;0`, `MEMBER;1;Ada Lovelace`,
    `LOAN;1;1;1;2026-01-01;2026-01-15;` (the last field is the return date, empty while open).
14. Quit and start again: media, members and loans are still there.
15. Borrowing a borrowed medium prints `Already borrowed: <title>` and the program goes on;
    an unknown medium number prints `No such medium: <nr>`, an unknown member `No such member: <nr>`.
    `borrow` checks in this order: the medium exists, the member exists, the medium is available.
16. No `shelfy.txt` yet: the program starts with an empty library, no exception.
17. `fees <date>` prints one line per open loan with `fee <amount>` (two decimals, dot as separator);
    a book and an audiobook overdue by the same days show different amounts.
