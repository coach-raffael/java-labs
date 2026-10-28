# Lab Module 18, Part A: ship shelfy 2.3

Starter: the code of shelfy 2.2 with the version number 2.3.0, the `pom.xml` of Day 4 and a
`package.sh` with the four steps as TODOs. Module 17 showed every command.

```
./mvnw verify
./package.sh
```

## Acceptance criteria

1. `./mvnw verify` builds `target/dist/shelfy-2.3.0.jar` with `Main-Class` and `Class-Path` in its
   manifest and copies the dependencies to `target/dist/lib/`. `java -jar target/dist/shelfy-2.3.0.jar serve`
   starts the app, `curl localhost:8080/health` shows `"version":"2.3.0"`.
2. `./package.sh` runs `jdeps`, `jlink`, `jpackage --type app-image` and, on Linux, `jpackage --type rpm`
   or `deb` built from the app image. The runtime in the package contains only the modules `jdeps` named
   (compare `target/pkg/shelfy/lib/runtime/release` with the `jdeps` output).
3. After `sudo rpm -i target/pkg/shelfy-2.3.0-1.x86_64.rpm` or `sudo apt install ./target/pkg/shelfy_2.3.0_amd64.deb`,
   `/opt/shelfy/bin/shelfy serve` starts the app on `PORT` (default 8080) and `/health` reports version
   2.3.0, with no `java` on the `PATH`. The package removes cleanly (`sudo rpm -e shelfy`, `sudo apt remove shelfy`).
   On Codespaces and macOS the app image counts as the package: `target/pkg/shelfy/bin/shelfy serve`.
4. `./checks/day5.sh` from the repository root prints `PASS` for Part A.

`shelfy/v2.3` in this repository is the finished state, for comparison after you are done.
