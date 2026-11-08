#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p target/classes target/test-classes
javac -encoding UTF-8 --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar -d target/classes src/main/java/ph/motorph/web/*.java
javac -encoding UTF-8 --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes -d target/test-classes src/test/java/ph/motorph/web/*.java
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.EmployeeServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.AttendanceServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.WorkflowServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.FirestoreRestClientTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.FirestoreWorkflowStoreTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.FirestoreEmployeeScheduleStoreTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.RbacPolicyTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.PayrollPeriodAndHistoryTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.IdentityAndSeatTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.PayrollGoldenTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.FirestorePayrollStoreTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.TimesheetServiceTest
MOTORPH_SWP_ENDPOINT=http://127.0.0.1:9/ java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.AppServerIntegrationTest

# Production entry point must refuse to serve process-memory data without Firebase.
server_error=$(mktemp)
if timeout 8s env -u FIREBASE_PROJECT_ID -u MOTORPH_TEST_MODE java --add-modules jdk.httpserver \
  -cp lib/gson-2.13.2.jar:target/classes ph.motorph.web.AppServer >/dev/null 2>"$server_error"; then
  rm -f "$server_error"
  echo "FAIL: production API unexpectedly started without Firebase configuration" >&2
  exit 1
else
  if ! grep -q "FIREBASE_PROJECT_ID is required" "$server_error"; then
    cat "$server_error" >&2
    rm -f "$server_error"
    echo "FAIL: API failed for a reason other than the Firebase-required guard" >&2
    exit 1
  fi
  rm -f "$server_error"
  echo "PASS: production API refuses local-memory startup without Firebase configuration"
fi
