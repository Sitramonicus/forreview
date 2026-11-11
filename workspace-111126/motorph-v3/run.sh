#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) S=";" ;; *) S=":" ;; esac
if [ ! -d node_modules ]; then
  echo "Dependencies are not installed. Run: npm install" >&2
  exit 1
fi
sh tools/build.sh
npm run build
if [ -z "${FIREBASE_PROJECT_ID:-}" ] && [ -f .env.local ]; then
  firebase_api_key=$(sed -n 's/^VITE_FIREBASE_API_KEY=//p' .env.local | head -n 1)
  case "$firebase_api_key" in
    ''|replace-*|PASTE_*|__*) ;;
    *) FIREBASE_PROJECT_ID=$(sed -n 's/^VITE_FIREBASE_PROJECT_ID=//p' .env.local | head -n 1); export FIREBASE_PROJECT_ID ;;
  esac
fi
WEB_ROOT=dist exec java --add-modules jdk.httpserver -cp "lib/gson-2.13.2.jar${S}lib/bcprov-jdk18on-1.78.1.jar${S}lib/google/*${S}target/classes" ph.motorph.svc.AppServer
