# shelfy-broken

shelfy 2.2 with eight planted mistakes (Day 5, Modules 16 and 18). Repair it here;
`shelfy/broken` keeps the untouched copy for a second try. Every mistake can be found with what Days 1 to 4
taught: read the code, run the tests, run the app, read the logs.

```
./mvnw verify                      # BrokenTest is red: read every failure
./mvnw -q compile 2>&1 | grep warning   # the compiler points at one of them
grep -n '==' src/main/java/shelfy/*.java
```

Module 16 names the eight categories: `==` on strings, `equals` without `hashCode`, a swallowed
exception, a resource that is never closed, a raw type with an unchecked cast, `null` as an answer,
static mutable state, and configuration that is read but not used.

`BrokenTest` calls `library.memberName(int)` and expects `"member 42"` for an unknown number 42: keep that
method when you repair `findMember`, and let it answer with a text instead of crashing.

When you are done (Module 18, Part B): `./mvnw verify` is green, `?available=true` filters,
120 requests against PostgreSQL go through, the log shows the database error instead of silence,
and `FIXES.md` lists the eight fixes in one line each (mistake, file and line, fix).
