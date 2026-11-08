#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) S=";" ;; *) S=":" ;; esac
mkdir -p target/classes target/test-classes
javac --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar -d target/classes src/main/java/ph/motorph/web/*.java
javac --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes -d target/test-classes src/test/java/ph/motorph/web/*.java
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.EmployeeServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.AttendanceServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.WorkflowServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.FirestoreRestClientTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.FirestoreWorkflowStoreTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.StatutoryCalculator2026Test
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.CutoffPayrollServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.AccessPolicyTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.ClockServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.AttendanceMetricsTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.AuditLogTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.GovernmentDetailsTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.LocalStateFileTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.AppServerIntegrationTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar${S}target/classes${S}target/test-classes ph.motorph.web.ScaleTest
