# Module 14, exercise 3: three errors you will meet

Each script starts shelfy 2.2 from `day4/shelfy` (build it first with `./mvnw -q verify` in that
directory) in a way that fails. Run one, read the message from the top, write the cause in one
sentence, then make the same start work by changing only the environment or the command.
Module 14.7 shows all three messages.

```
./day4/errors/port.sh        # the port is taken
./day4/errors/driver.sh      # the JDBC driver is missing from the classpath
./day4/errors/password.sh    # the database says no
```

The scripts expect PostgreSQL on `localhost:5432` with the database `shelfy` and the user `shelfy`
(password `shelfy-lab`), as set up in Module 14.1 and 14.2.
