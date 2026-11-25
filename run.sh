#!/usr/bin/env bash
# Compile the whole project and start the web dashboard. One command, no Maven/Gradle.
# POSIX lab machines (Linux/macOS). On Windows use run.bat instead.
set -e
cd "$(dirname "$0")"

SQLITE="lib/sqlite-jdbc-3.46.1.3.jar"

mkdir -p out
echo "Compiling (Java 17)..."
javac --release 17 -cp "$SQLITE" -d out $(find src -name "*.java")

echo "Starting Smart Farm Bot on http://localhost:8080/login.html ..."
java --enable-native-access=ALL-UNNAMED -cp "out:$SQLITE" farmbot.web.ApiServer
