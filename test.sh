#!/usr/bin/env bash
# Compile and run the JUnit 5 test suite. POSIX machines; on Windows use test.bat.
set -e
cd "$(dirname "$0")"

SQLITE="lib/sqlite-jdbc-3.46.1.3.jar"
JUNIT="lib/junit-platform-console-standalone-1.11.3.jar"

mkdir -p out out-test
echo "Compiling sources..."
javac --release 17 -cp "$SQLITE" -d out $(find src -name "*.java")
echo "Compiling tests..."
javac --release 17 -cp "out:$JUNIT:$SQLITE" -d out-test $(find test -name "*.java")

echo "Running tests..."
java --enable-native-access=ALL-UNNAMED -jar "$JUNIT" execute \
  --class-path "out:out-test:$SQLITE" --scan-class-path
