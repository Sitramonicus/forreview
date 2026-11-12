#!/usr/bin/env sh
# Compile every main source (all packages under ph/motorph) and, optionally, the tests.
set -eu
cd "$(dirname "$0")/.."
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) S=";" ;; *) S=":" ;; esac
mkdir -p target/classes target/test-classes
find src/main/java -name '*.java' > target/main-sources.txt
# Resources travel with the classes: the employee file is read from the classpath at startup.
mkdir -p target/classes
cp -r src/main/resources/. target/classes/
javac -encoding UTF-8 --add-modules jdk.httpserver -cp "lib/gson-2.13.2.jar${S}lib/bcprov-jdk18on-1.78.1.jar${S}lib/google/*" -d target/classes @target/main-sources.txt
if [ "${1:-}" = "--with-tests" ]; then
  find src/test/java -name '*.java' > target/test-sources.txt
  javac -encoding UTF-8 --add-modules jdk.httpserver \
    -cp "lib/gson-2.13.2.jar${S}lib/bcprov-jdk18on-1.78.1.jar${S}lib/google/*${S}target/classes" \
    -d target/test-classes @target/test-sources.txt
fi
