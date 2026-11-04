#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p target/classes target/test-classes
javac --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar -d target/classes src/main/java/ph/motorph/web/*.java
javac --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes -d target/test-classes src/test/java/ph/motorph/web/*.java
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.EmployeeServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.AttendanceServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.WorkflowServiceTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.FirestoreRestClientTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.FirestoreWorkflowStoreTest
java --add-modules jdk.httpserver -cp lib/gson-2.13.2.jar:target/classes:target/test-classes ph.motorph.web.AppServerIntegrationTest
