# tools/

External-reference probes used to validate the payroll engine against
<https://www.sweldongpinoy.com>. **Not part of the product build**, not run by `./test.sh`, and never
shipped to the browser.

| File | What it does | Runs in `./test.sh`? |
|---|---|---|
| `generate_fixtures.py` | **Reads MotorPH's employee CSV and writes `InMemoryEmployeeStore.java`** — all 34 rows, real numbers, real pay. Run it after the CSV changes. Fails loudly on an unknown position rather than guessing a department. | No — the generated Java file is committed, so the build does not need the CSV or Python |
| `sweldongpinoy-crosscheck.py` | Posts this app's 34 employee pay profiles to the live site *and* to our API, then compares every statutory line to the centavo | No — needs the internet |

### `generate_fixtures.py`

```sh
python3 tools/generate_fixtures.py        # rewrites src/main/java/ph/motorph/web/InMemoryEmployeeStore.java
./test.sh                                 # confirm the build still passes
```

Input: `uploads/Copy of MotorPH_Employee Data - Employee Details.csv`.
It copies `Employee #`, `First Name` + `Last Name`, `Position`, `Basic Salary`, `Rice Subsidy`,
`Phone Allowance`, `Clothing Allowance` and `Address` verbatim. It derives **department from
position** (the CSV has no department column) and sets status `Active` for everyone (the CSV's
`Status` column is Regular/Probationary — employment type, not active/on-leave).

**Never hand-edit `InMemoryEmployeeStore.java`** — the next generator run overwrites it.

Requires network access and a running API:

```sh
MOTORPH_TEST_MODE=true HOST=127.0.0.1 PORT=8183 java --add-modules jdk.httpserver \
  -cp lib/gson-2.13.2.jar:target/classes ph.motorph.web.AppServer &
python3 tools/sweldongpinoy-crosscheck.py
```

See `docs/sweldongpinoy-crosscheck.md` for what the comparison proves, what it does not, and why the
site is a reference rather than a dependency.
