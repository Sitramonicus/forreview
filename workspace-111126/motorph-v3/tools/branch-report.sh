#!/usr/bin/env sh
# Prints the branch assignment and the org chart exactly as the application computes them.
set -eu
cd "$(dirname "$0")/.."
sh tools/build.sh
mkdir -p target/smoke
cat > target/smoke/BranchReport.java <<'JAVA'
import ph.motorph.o_oop.*;
public class BranchReport {
    public static void main(String[] args) {
        EmployeeImport imp = EmployeeImport.fromClasspath();
        imp.notes().forEach(note -> System.out.println(note));
        OrgChart chart = new OrgChart(imp.employees());
        System.out.println("\nemployees=" + imp.employees().size() + " problems=" + chart.problems().size());
        chart.headcountByBranch().forEach((branch, count) -> System.out.println("  " + branch + ": " + count));
        System.out.println();
        for (Employee e : imp.employees()) {
            System.out.printf("%s  %-24s %-14s %-30s %-12s sup=%s%n", e.getEid(), e.getDisplayName(),
                e.getBranchName(), e.getPosition(), chart.roleOf(e.getEid()), chart.supervisorOf(e.getEid()).orElse("-"));
        }
    }
}
JAVA
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) S=";" ;; *) S=":" ;; esac
javac -encoding UTF-8 -cp "target/classes" -d target/smoke target/smoke/BranchReport.java
exec java -cp "target/classes${S}target/smoke${S}lib/gson-2.13.2.jar" BranchReport
