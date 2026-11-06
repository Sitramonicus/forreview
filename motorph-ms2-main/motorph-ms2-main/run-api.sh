#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) S=";" ;; *) S=":" ;; esac
mkdir -p target/classes
javac --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar -d target/classes src/main/java/ph/motorph/web/*.java
if [ -z "${FIREBASE_PROJECT_ID:-}" ] && [ -f .env.local ]; then
  firebase_api_key=$(sed -n 's/^VITE_FIREBASE_API_KEY=//p' .env.local | head -n 1)
  case "$firebase_api_key" in
    ''|replace-*|PASTE_*|__*) ;;
    *) FIREBASE_PROJECT_ID=$(sed -n 's/^VITE_FIREBASE_PROJECT_ID=//p' .env.local | head -n 1); export FIREBASE_PROJECT_ID ;;
  esac
fi
exec java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes ph.motorph.web.AppServer
