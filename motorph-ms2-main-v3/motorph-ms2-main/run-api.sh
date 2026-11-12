#!/usr/bin/env sh
# Runs the MotorPH API and web UI.
#
# The classpath carries three things: Gson (JSON), BouncyCastle (Argon2id, which the password hashes need), and the
# compiled classes. The employee file and the sample payslip ship inside target/classes as resources.
set -eu
cd "$(dirname "$0")"
if [ -f .env ]; then set -a; . ./.env; set +a; fi
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) S=";" ;; *) S=":" ;; esac

# Build first: compiling here means a fresh clone runs with one command.
sh tools/build.sh

# The session pepper. Refusing to invent one silently is the point: a pepper the code carries is not a secret.
if [ -z "${MOTORPH_PEPPER:-}" ]; then
  if [ -f .env.local ]; then
    pepper=$(sed -n 's/^MOTORPH_PEPPER=//p' .env.local | head -n 1)
    case "$pepper" in
      ''|replace-*|PASTE_*|__*) ;;
      *) MOTORPH_PEPPER="$pepper"; export MOTORPH_PEPPER ;;
    esac
  fi
fi

exec java --add-modules jdk.httpserver \
  -cp "lib/gson-2.13.2.jar${S}lib/bcprov-jdk18on-1.78.1.jar${S}lib/google/*${S}target/classes" \
  ph.motorph.svc.AppServer
