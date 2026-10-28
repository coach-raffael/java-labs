# java-labs

Lab files for the course "Java Programming Fundamentals" in the IT made simple community.

Course community: https://www.skool.com/coach-raffael-it-made-simple-5657

The course builds one application, **shelfy**, over five days: a small library that lends books and
audiobooks. It starts as one source file with arrays (Day 1), becomes a package of classes and records
with a text file as storage (Day 2), a tested Maven project (Day 3), a web app with JSON and PostgreSQL
(Day 4) and finally an installable native package (Day 5). You write the code yourself in the lab lessons;
this repository gives you the starting points, the check scripts, the exercise files and the finished
state of every day for comparison.

## Layout

```
java-labs
├── day1
│   ├── hello/Hello.java         Modules 00 and 01: your first program (java Hello.java)
│   ├── fix/                     Module 01, exercise 1: three files with one compiler error each
│   └── shelfy/Shelfy.java       Module 03 (lab): starter with the acceptance criteria as a comment
├── day2
│   ├── npe/Catalog.java         Module 04, exercise 3: find the null
│   └── shelfy/                  Module 07 (lab): starter (Main.java with the menu loop, Storage.java)
├── day3
│   ├── redtest/                 Module 10, exercise 3: a Maven project with one red test
│   └── shelfy/                  Module 11 (lab): shelfy 2.0 plus pom.xml and the Maven Wrapper
├── day4
│   ├── sql/schema.sql           Module 14: the three tables for psql
│   ├── errors/                  Module 14, exercise 3: three scripts that start shelfy the wrong way
│   └── shelfy/                  Module 15 (lab): shelfy after Module 14, /loans still to do
├── day5
│   ├── shelfy/                  Module 18 (lab, Part A): shelfy 2.2 as 2.3.0, package.sh as a skeleton
│   ├── shelfy-broken/           Modules 16 and 18 (Part B): eight planted mistakes, BrokenTest is red
│   ├── java21/run.sh            Module 17, exercise 4: start the JAR with a Java 21 runtime
│   └── oom/Flood.java           Module 17.2: fill the heap and read the OutOfMemoryError
├── shelfy
│   ├── v1.0/ v2.0/ v2.1/ v2.2/ v2.3/   the finished state of each day, for comparison after the lab
│   └── broken/                  the untouched copy of day5/shelfy-broken
├── checks                       day1.sh to day5.sh: acceptance checks for the lab lessons; day4-break.sh plants a fault
└── .devcontainer                GitHub Codespaces: JDK 25, Maven and PostgreSQL 18 as a second container
```

Every lab lesson ends with `./checks/dayN.sh` from the repository root. The script prints one line per
acceptance criterion and `PASS` or `FAIL` at the end. Read a failing line as a hint, not as the solution:
the course workbook holds the full solution of every lab, and `shelfy/v<version>` the finished code.
The finished state of a day is the starting point of the next one, so look at it only after your own attempt.

## Usage

On your lab machine (Module 00 sets it up, three ways: your own VPS, your own computer or a Codespace):

```
mkdir -p ~/labs && cd ~/labs
git clone https://github.com/coach-raffael/java-labs.git
cd java-labs
java day1/hello/Hello.java
```

You need JDK 25 (`java`, `javac`, `jshell`, `jdeps`, `jlink`, `jpackage`; the Temurin or distribution
package, not a JRE). Maven comes with the Maven Wrapper in every project (`./mvnw`, downloads Maven 3.10
on first use). Day 4 and 5 need PostgreSQL 18 (Module 14.1) and, for native packages, `rpm-build`
(Rocky, Alma) or `fakeroot` (Ubuntu). In a Codespace the database runs as a second container; the
variables `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` and `TEST_DATABASE_URL` are set.

### shelfy

Versions and what each day adds:

| Version | Day | Code | New |
| --- | --- | --- | --- |
| 1.0 | 1 | `shelfy/v1.0/Shelfy.java` | one compact source file, arrays, a menu in a loop, `IO.readln` |
| 2.0 | 2 | `shelfy/v2.0/src/shelfy/` | package `shelfy`, `Medium` with `Book` and `Audiobook`, `Member`, record `Loan`, sealed `LoanStatus`, `Storage` with `InMemoryStorage` and `FileStorage` (`shelfy.txt`), own exceptions |
| 2.1 | 3 | `shelfy/v2.1/` (Maven) | Maven Wrapper, JUnit 6, collections, streams, `Optional`, `java.time`; commands `available`, `overdue`, `stats`, `search` |
| 2.2 | 4 | `shelfy/v2.2/` | Strategy, Factory, Observer, Builder and dependency injection by hand; `HttpServer` with five endpoints and Jackson 3; PostgreSQL through JDBC; configuration from the environment; logging to stdout |
| 2.3 | 5 | `shelfy/v2.3/` | the code of 2.2 as an executable JAR (`target/dist`), `package.sh` with `jdeps`, `jlink` and `jpackage` |

Run shelfy 2.0 (Day 2) with `javac` and `java`:

```
cd shelfy/v2.0
javac -d out src/shelfy/*.java
java -cp out shelfy.Main
```

Run shelfy 2.1 and later with Maven:

```
cd shelfy/v2.2
./mvnw verify
java -cp 'target/classes:target/dependency/*' shelfy.Main serve
curl -s localhost:8080/health
```

shelfy 2.3 puts its dependencies next to the JAR instead: `java -jar target/dist/shelfy-2.3.0.jar serve`.

The console commands (`console` instead of `serve`, or one command as the argument): `list`, `add`, `member <name>`,
`borrow <mediumNr> <memberNr> [yyyy-mm-dd]`, `return <mediumNr> [yyyy-mm-dd]`, `fees <yyyy-mm-dd>`, and from 2.1 on
`available`, `overdue <yyyy-mm-dd>`, `stats`, `search <text>`. The optional date lets you test late fees without waiting.

Endpoints of shelfy 2.2 and 2.3:

| Endpoint | Purpose |
| --- | --- |
| `GET /health` | status, version, storage (`memory`, `file` or `postgres`), counters; 503 when the database is gone |
| `GET /books` | all media with `available`; filter with `?available=true` or `false` |
| `POST /books` | create a medium: JSON `{"title": "...", "author": "...", "isbn": "...", "kind": "BOOK"}` (`kind` optional; an `AUDIOBOOK` needs `durationMinutes`), answers 201 |
| `POST /loans` | lend a medium: JSON `{"bookId": 1, "memberId": 1}`, answers 201, 404 or 409 |
| `PATCH /loans/<id>` | return it: JSON `{"returned": true}`, answers with `daysOverdue` and `lateFee` |

| Variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | `8080` | port the server listens on |
| `DATABASE_URL` | empty | `jdbc:postgresql://host:5432/shelfy`; empty means the text file |
| `DATABASE_USER`, `DATABASE_PASSWORD` | empty | credentials, kept out of the URL so that the URL may be logged |
| `DATA_FILE` | `shelfy.txt` | path of the text file when no database is used |
| `LOG_LEVEL` | `INFO` | `FINE`, `INFO`, `WARNING` or `SEVERE` |
| `LOG_FORMAT` | `text` | `text` or `json`, one line per request and per library event on stdout |
| `LOAN_DAYS` | `14` | loan period in days |

`--storage memory|file|postgres` as the first arguments overrides the storage (`postgres` when
`DATABASE_URL` is set, else `file`). The app creates its three tables at start when they are missing;
`day4/sql/schema.sql` holds the same statements for `psql`. Members exist only through the console or
`psql` until the Spring Boot course adds `POST /members`.

Run the tests of a version without a database:

```
cd shelfy/v2.2
./mvnw verify
```

`PostgresStorageTest` runs when `TEST_DATABASE_URL` (plus `TEST_DATABASE_USER`, `TEST_DATABASE_PASSWORD`)
points to a database you may empty.

### Checks

| Script | Lab | Needs |
| --- | --- | --- |
| `checks/day1.sh` | Module 03 | `day1/shelfy/Shelfy.java` |
| `checks/day2.sh` | Module 07 | `day2/shelfy/src/shelfy/*.java` |
| `checks/day3.sh` | Module 11 | `day3/shelfy` as a Maven project |
| `checks/day4.sh` | Module 15 | `day4/shelfy`, PostgreSQL, `psql`; `checks/day4-break.sh` plants the fault for Part B |
| `checks/day5.sh` | Module 18 | `day5/shelfy` built and packaged, `day5/shelfy-broken` repaired with `FIXES.md` |

### Passwords in this repository

The PostgreSQL password `shelfy-lab` and the user `shelfy` are training values for your lab machine.
Replace them with your own values when you move anything out of the lab.

## License

The code in this repository is licensed under the MIT License, see LICENSE.
`mvnw` and `mvnw.cmd` are the Apache Maven Wrapper scripts (Apache License 2.0, see their headers).
The course text and workbooks are not part of this repository.

Author: Raffael Hühnerschulte
