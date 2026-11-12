#!/usr/bin/env sh
# Optional audits. Not part of ./test.sh -- these need downloads and are slower.
# ESLint covers the browser code, Checkstyle covers the Java. Both are configured to
# match the project's own house style, not Google's or Sun's.
set -eu
cd "$(dirname "$0")/.."

fail=0

echo "---- ESLint (web + web tests) ----"
if command -v npx >/dev/null 2>&1 && [ -d node_modules/eslint ]; then
  if node_modules/.bin/eslint "web/**/*.js" "test/web/**/*.js"; then
    echo "ESLint: clean"
  else
    echo "ESLint: findings above"
    fail=$((fail + 1))
  fi
else
  echo "ESLint: skipped (npm install first)"
fi

echo "---- Checkstyle (src/main/java) ----"
if [ -f tools/lint/checkstyle.jar ]; then
  if java -jar tools/lint/checkstyle.jar -c tools/lint/checkstyle.xml src/main/java 2>&1 | tee /tmp/motorph-checkstyle.txt | grep -q "^\[ERROR\]"; then
    grep "^\[ERROR\]" /tmp/motorph-checkstyle.txt | head -40
    fail=$((fail + 1))
  else
    echo "Checkstyle: clean"
  fi
else
  echo "Checkstyle: skipped (tools/lint/checkstyle.jar missing)"
fi

echo "---- PMD (errorprone + bestpractices) ----"
PMD_HOME="${PMD_HOME:-}"
if [ -z "$PMD_HOME" ]; then
  for candidate in "$HOME/.cache/pmd/pmd-bin-7.0.0" /tmp/pmd/pmd-bin-7.0.0; do
    [ -d "$candidate" ] && PMD_HOME="$candidate" && break
  done
fi
if [ -n "$PMD_HOME" ] && [ -x "$PMD_HOME/bin/pmd" ]; then
  # errorprone catches real bugs; bestpractices catches dead code. The style-heavy
  # rules (literals-in-if, duplicate literals, field/method name echo) are off by default here.
  if "$PMD_HOME/bin/pmd" check -d src/main/java -R category/java/errorprone.xml -f text --no-cache 2>&1 \
      | tee /tmp/motorph-pmd.txt | grep -qE "UseLocaleWithCaseConversions|UnusedPrivateMethod"; then
    grep -E "UseLocaleWithCaseConversions|UnusedPrivateMethod" /tmp/motorph-pmd.txt | head -20
    fail=$((fail + 1))
  else
    echo "PMD: clean on the correctness rules (locale bugs, unused private members)"
  fi
else
  echo "PMD: skipped (set PMD_HOME to a pmd-bin-7.x directory)"
fi

echo "---- SpotBugs ----"
SB_JAR="${SPOTBUGS_JAR:-}"
if [ -z "$SB_JAR" ]; then
  for candidate in "$HOME/.cache/spotbugs"/spotbugs-4.8.3/lib/spotbugs.jar /tmp/spotbugs-4.8.3/lib/spotbugs.jar; do
    [ -f "$candidate" ] && SB_JAR="$candidate" && break
  done
fi
if [ -n "$SB_JAR" ] && [ -d target/classes ]; then
  if java -jar "$SB_JAR" -textui -effort:max -low \
        -exclude tools/lint/spotbugs-exclude.xml \
        -auxclasspath "lib/gson-2.13.2.jar:lib/bcprov-jdk18on-1.78.1.jar" \
        target/classes 2>&1 | tee /tmp/motorph-spotbugs.txt | grep -qE "Possible null|Dead store|Bad attempt|Inefficient"; then
    grep -E "Possible null|Dead store|Bad attempt|Inefficient" /tmp/motorph-spotbugs.txt | head -20
    fail=$((fail + 1))
  else
    echo "SpotBugs: clean on the correctness detectors"
  fi
else
  echo "SpotBugs: skipped (set SPOTBUGS_JAR, and run tools/build.sh first)"
fi

echo "---- Lighthouse ----"
if [ -d node_modules/lighthouse ] && curl -fsS -o /dev/null http://127.0.0.1:8080/ 2>/dev/null; then
  CHROME=$(ls -d "$HOME"/.cache/ms-playwright/chromium-*/chrome-linux64/chrome 2>/dev/null | head -1)
  CHROME_PATH="${CHROME:-${CHROME_PATH:-}}" \
    node_modules/.bin/lighthouse http://127.0.0.1:8080/ \
      --only-categories=accessibility,best-practices \
      --chrome-flags="--headless --no-sandbox --disable-dev-shm-usage --disable-gpu" \
      --output=json --output-path=/tmp/motorph-lighthouse.json --quiet || true
  if [ -f /tmp/motorph-lighthouse.json ]; then
    node -e '
      const r = require("/tmp/motorph-lighthouse.json");
      for (const [k, v] of Object.entries(r.categories)) console.log(`  ${k}: ${Math.round(v.score * 100)}`);
      const bad = Object.values(r.categories).flatMap(c => c.auditRefs)
        .filter(a => r.audits[a.id].score !== null && r.audits[a.id].score < 1 && a.weight > 0)
        .map(a => a.id);
      console.log(bad.length ? `  failing: ${bad.join(", ")}` : "  nothing scoring below 1.0");
    '
  fi
else
  echo "Lighthouse: skipped (start the server first: sh tools/serve.sh, or see SETUP.md)"
fi

if [ "$fail" -eq 0 ]; then
  echo "-- lint clean --"
else
  echo "-- lint finished with $fail failing group(s) --"
  exit 1
fi
