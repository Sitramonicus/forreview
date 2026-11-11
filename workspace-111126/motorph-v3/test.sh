#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) S=";" ;; *) S=":" ;; esac

sh tools/build.sh --with-tests
CP="lib/gson-2.13.2.jar${S}lib/bcprov-jdk18on-1.78.1.jar${S}lib/google/*${S}target/classes${S}target/test-classes"

fail=0
pass=0
for f in $(find src/test/java -name '*Test.java' | sort); do
  cls=$(printf '%s' "$f" | sed 's|src/test/java/||; s|/|.|g; s|\.java$||')
  if java --add-modules jdk.httpserver -cp "$CP" "$cls" > /tmp/motorph-test-out.txt 2>&1 && ! grep -q "^FAIL" /tmp/motorph-test-out.txt; then
    tail -n 1 /tmp/motorph-test-out.txt | sed "s|^|$cls: |"
    pass=$((pass + 1))
  else
    echo "FAILED: $cls"
    cat /tmp/motorph-test-out.txt
    fail=$((fail + 1))
  fi
done

if command -v python3 >/dev/null 2>&1; then
  echo "---- Python tool tests ----"
  if python3 -m unittest discover -s test/python -p 'test_*.py' > /tmp/motorph-python-out.txt 2>&1; then
    tail -n 2 /tmp/motorph-python-out.txt | sed 's/^/python: /'
    pass=$((pass + 1))
  else
    echo "Python tool tests FAILED"
    cat /tmp/motorph-python-out.txt
    fail=$((fail + 1))
  fi
fi

if command -v node >/dev/null 2>&1; then
  echo "---- web unit tests ----"
  if node --test test/web/*.test.js 2>&1 | tee /tmp/motorph-web-out.txt | tail -n 8 | grep -q "^# fail 0"; then
    grep -E "^# (tests|pass|fail)" /tmp/motorph-web-out.txt | sed 's/^# /web: /'
    pass=$((pass + 1))
  else
    echo "web unit tests FAILED"
    grep -E "^not ok" /tmp/motorph-web-out.txt | head -10
    fail=$((fail + 1))
  fi
fi

# The browser suite needs playwright-core and a Chromium binary; skip it cleanly when absent
# instead of failing a machine that has neither.
if [ "${MOTORPH_SKIP_E2E:-0}" = "1" ]; then
  echo "e2e: skipped (MOTORPH_SKIP_E2E=1)"
elif [ -d node_modules/playwright-core ] && [ -x "${CHROMIUM_PATH:-/home/user/.cache/ms-playwright/chromium-1248/chrome-linux64/chrome}" ]; then
  echo "---- browser e2e ----"
  if node test/e2e/local-workflows.e2e.js > /tmp/motorph-e2e-out.txt 2>&1; then
    tail -n 1 /tmp/motorph-e2e-out.txt
    pass=$((pass + 1))
  else
    echo "browser e2e FAILED"
    grep -E "^FAIL|^PASS|^[0-9]/5" /tmp/motorph-e2e-out.txt | tail -8
    fail=$((fail + 1))
  fi
else
  echo "e2e: skipped (playwright-core or Chromium not present)"
fi

echo "----"
echo "suites passed: $pass   failed: $fail"
[ "$fail" -eq 0 ]
