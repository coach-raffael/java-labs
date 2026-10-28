#!/usr/bin/env bash
# Module 17, exercise 4: start the shelfy 2.3 JAR with a Java 21 runtime and read the error.
# Downloads Eclipse Temurin 21 (JRE, about 50 MB) once into /tmp/temurin21 through the Adoptium API,
# then runs the JAR from day5/shelfy with it. Nothing is installed; delete /tmp/temurin21 afterwards.
# Run from the repository root after building day5/shelfy:  ./day5/java21/run.sh
#
# Docker users can run the same test without a download:
#   docker run --rm -v "$PWD/day5/shelfy/target/dist:/app" eclipse-temurin:21-jre java -jar /app/shelfy-2.3.0.jar serve
set -u
LAB="$(cd "$(dirname "$0")/../.." && pwd)"
JAR="$LAB/day5/shelfy/target/dist/shelfy-2.3.0.jar"
if [ ! -f "$JAR" ]; then
  echo "Build the JAR first: cd day5/shelfy && ./mvnw -q verify"
  exit 1
fi

ARCH="$(uname -m)"
case "$ARCH" in
  x86_64) ARCH=x64 ;;
  aarch64 | arm64) ARCH=aarch64 ;;
esac
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
case "$OS" in
  darwin) OS=mac ;;
esac
DIR=/tmp/temurin21
find_java21() {
  ls -d "$DIR"/*/bin/java "$DIR"/*/Contents/Home/bin/java 2>/dev/null | head -n 1
}
JAVA21="$(find_java21)"
if [ -z "$JAVA21" ]; then
  mkdir -p "$DIR"
  echo "Downloading Temurin 21 JRE for $OS/$ARCH ..."
  curl -fsSL -o "$DIR/jre.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/21/ga/$OS/$ARCH/jre/hotspot/normal/eclipse?project=jdk" || exit 1
  tar -xzf "$DIR/jre.tar.gz" -C "$DIR"
  JAVA21="$(find_java21)"
fi
echo "Using: $JAVA21"
"$JAVA21" -version
echo
echo "---- java -jar shelfy-2.3.0.jar with Java 21:"
"$JAVA21" -jar "$JAR" serve
echo
echo "Read the message: which class file version does the JAR have, which one does this runtime accept?"
echo "Module 17.7 has the table. Clean up with: rm -rf $DIR"
