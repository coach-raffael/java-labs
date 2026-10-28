# Lab Module 15: shelfy 2.2 serves HTTP

Starter for the lab lesson of Day 4: shelfy after Module 14. `GET /health`, `GET /books` and
`POST /books` work, PostgreSQL, configuration and logging are in place. `POST /loans` and
`PATCH /loans/<id>` answer 501 until you write them (`HttpApi.loans`).

```
./mvnw verify
java -cp 'target/classes:target/dependency/*' shelfy.Main serve
curl -s localhost:8080/health
```

Check your work from the repository root with `./checks/day4.sh` (needs PostgreSQL, see below),
then find the planted fault with `./checks/day4-break.sh` (Part B).

## Acceptance criteria

### Part A, the contract

| Method and path | Request | Response | Errors |
| --- | --- | --- | --- |
| `GET /health` | none | 200 `{"status":"ok","version":"2.2.0","storage":"postgres","books":12,"openLoans":3,"overdueLoans":1}` | 503 `{"status":"error",...,"error":"database unreachable"}` when the database is gone |
| `GET /books` | optional `?available=true` or `false` | 200 list of media (`id`, `kind`, `title`, `author`, `isbn`, `durationMinutes`, `available`), by id | 400 for any other value |
| `POST /books` | `{"title":"...","author":"...","isbn":"...","kind":"BOOK"}` (`kind` optional, `durationMinutes` needed for `AUDIOBOOK`) | 201 with the medium and `Location: /books/<id>` | 400 missing title, author or isbn, title over 200 characters, unknown kind; 409 ISBN already there |
| `POST /loans` | `{"bookId":1,"memberId":1}` | 201 with `id`, `bookId`, `memberId`, `borrowedOn`, `due`, `status` and `Location: /loans/<id>` | 400 missing field, 404 unknown medium or member, 409 medium already out |
| `PATCH /loans/<id>` | `{"returned":true}` | 200 with the loan, `status` `RETURNED`, `returnedOn`, `daysOverdue`, `lateFee` | 400 any other body, 404 unknown loan, 409 already returned |

1. Every error body is `{"error":"..."}`. Unknown paths answer 404, a wrong method 405 with an `Allow` header.
2. `PORT`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `DATA_FILE`, `LOG_LEVEL`, `LOG_FORMAT`
   and `LOAN_DAYS` work as in Module 14.5; without `DATABASE_URL` the app runs on `shelfy.txt`.
3. Stop the app and start it again: books and loans in PostgreSQL are still there.
4. `./mvnw verify` is green and includes a test for `POST /loans` and `PATCH /loans/<id>` in `HttpApiTest`.
5. `./checks/day4.sh` prints `PASS`. It builds the project, starts the app on a free port with the
   database from `TEST_DATABASE_URL` (default `jdbc:postgresql://localhost:5432/shelfy`, user `shelfy`,
   password `shelfy-lab`; override with `TEST_DATABASE_USER` and `TEST_DATABASE_PASSWORD`), calls every
   endpoint, restarts the app and checks that the data survived.

### Part B, find the planted fault

6. `./checks/day4-break.sh` starts the app with a broken environment. Find the cause with the
   messages of Module 14.7, explain it in two sentences, make the start work.

Members exist only through the console (`java -cp ... shelfy.Main member "Ada Lovelace"`) or through
`psql` until the Spring Boot course adds `POST /members`.
