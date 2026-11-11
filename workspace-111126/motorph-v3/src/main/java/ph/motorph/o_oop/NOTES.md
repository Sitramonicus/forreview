# o_oop

The objects: employee, org chart, the stores, the CSV importer.

- `EmployeeImport.fromClasspath()` reads `data/EmployeeDetails.csv`, 34 people. That file is the
  roster. There is no other roster and there must not be one.
- `Employee.getFullName()` is `"Last, First"`. `getName()` is `"First Last"`. Both get used; check
  which one before you print.
- `OrgChart` walks the supervisor column recursively. Latian's and De Leon's teams sit under
  Romualdez, so Romualdez sees both plus anyone under them.
- `InMemoryEmployeeStore` starts **empty**. Tests seed it from `TestRoster`. Do not put people in
  the constructor — that is how the fake data got in last time.
- The supervisor column in the CSV is unreliable in places. Branch assignment uses addresses,
  not supervisors.

## member notation

`Branch` and `Locality` converted (`br_`, `lc_`). `RolePolicy` is now `Policies` — it's a factory
returning `AccessPolicy.Role`, so calling it `Role` would fight the type it produces.

Enum constants (`MAIN`, `TAGUIG`, …) are untouched: renaming those is churn with no readability win,
and `Branch.TAGUIG` is already the clearest thing in the file.

persistence gotcha -- 2026-11-08
renamed Employee + AttendanceRecord fields to the ab_name style and it silently ate the data file.
gson writes by field name, so old keys loaded into nothing and the next save wrote the blanks back.
7421 attendance rows went to {"ar_iId":0} -- restored from data/backups.
fix: @SerializedName with the old key on every persisted class. java name follows §4, json key never moves.
verify by restarting + reading a row back. the test suite never touches local-demo.json so it stayed green.

o_oop conversion finished -- 2026-11-08
WorkSchedule was the annoying one -- public final fields read straight through, so the rename spilled
into AppServer, AttendanceMetrics and PayrollProfile. 17 sites. all mechanical once the receivers were
mapped, no getters to hide behind.
one near miss: a blanket replace on change.setBy also hit exchange.setAttribute on the HttpExchange.
caught by the compiler, not by me. worth grepping the receiver before any bulk rename.
StateFileRoundTripTest now covers schedules and day changes too. 20 checks.

restored EmployeeImport helpers -- 2026-11-08
same over-eager delete took nameOf, nameOfRow and assignBranch with it.
nameOf was pure redundancy -- the two lines above it already put both name orders in the map. dropped it.
nameOfRow and assignBranch rebuilt from their call sites. assignBranch is the centroid rule: average the
lat/lng of every direct report's city, pick the nearest field office, fall back to Main with a note.
branches came back Main 8 / Taguig 23 / Pasig 3 and every C-level is on Main, which is the rule.
