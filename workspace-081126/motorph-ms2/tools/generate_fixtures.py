"""Generates InMemoryEmployeeStore.java from MotorPH's Employee Details CSV.

The CSV is the source of truth: 34 employees with their real employee numbers,
names, positions, basic salaries and allowances. Nothing here is invented.

Run from the repo root:
    python3 tools/generate_fixtures.py
"""
import csv
import re
import pathlib

CSV = pathlib.Path("/home/user/uploads/Copy of MotorPH_Employee Data - Employee Details.csv")
OUT = pathlib.Path("src/main/java/ph/motorph/web/InMemoryEmployeeStore.java")

# MotorPH's file has no department column; departments are derived from the position
# so the directory screen has something meaningful to group by.
# MotorPH's file has no department column, so it is derived from the position. Every position
# string in the supplied file must match a prefix here; the generator asserts that it does.
# MotorPH's file has no department column, so it is derived from the position. Every one of the
# 18 positions in the supplied file is listed; the generator fails loudly if a new one appears.
DEPARTMENTS = [
    ("Chief Executive Officer", "Executive"),
    ("Chief Operating Officer", "Executive"),
    ("Chief Finance Officer", "Finance"),
    ("Chief Marketing Officer", "Marketing"),
    ("IT Operations and Systems", "IT"),
    ("HR Manager", "HR"),
    ("HR Team Leader", "HR"),
    ("HR Rank and File", "HR"),
    ("Accounting Head", "Accounting"),
    ("Account Manager", "Accounting"),
    ("Account Team Leader", "Accounting"),
    ("Account Rank and File", "Accounting"),
    ("Payroll Manager", "Payroll"),
    ("Payroll Team Leader", "Payroll"),
    ("Payroll Rank and File", "Payroll"),
    ("Sales & Marketing", "Sales"),
    ("Supply Chain and Logistics", "Supply Chain"),
    ("Customer Service and Relations", "Customer Success"),
]


def department_for(position: str) -> str:
    for needle, dept in DEPARTMENTS:
        if position.startswith(needle):
            return dept
    raise SystemExit(f"No department mapping for position: {position!r}")


def money(value: str) -> str:
    return value.replace(",", "").replace("₱", "").strip()


def name_keys(last, first):
    """Every shape a name can take in this file, normalised the same way.

    MotorPH writes compound surnames inconsistently: the person is "San Jose, Brad" in the name
    columns, but other rows refer to him as "San, Jose Brad". Both must resolve to 10007, so each
    name is indexed under all its plausible spellings instead of being parsed.
    """
    def norm(text):
        return " ".join(text.lower().replace(",", " ").split())

    keys = {norm(f"{last} {first}"), norm(f"{first} {last}"), norm(f"{last}, {first}")}
    # Also index the comma-split form: "San Jose, Brad" -> "San Jose Brad" is already covered,
    # and "Brad San Jose" -> fine; the awkward incoming form is "san jose brad", so add the
    # surname-with-internal-comma variants too.
    parts = last.split()
    if len(parts) > 1:
        keys.add(norm(f"{parts[0]}, {' '.join(parts[1:])} {first}"))
    return keys


def supervisor_numbers(rows):
    """Map every spelling of a person's name to their own employee number."""
    by_name = {}
    for row in rows:
        for key in name_keys(row["Last Name"].strip(), row["First Name"].strip()):
            by_name[key] = row["Employee #"].strip()
    return by_name


def main() -> None:
    rows = list(csv.DictReader(CSV.open(encoding="utf-8-sig")))
    supervisors = supervisor_numbers(rows)
    unresolved = set()
    lines = []
    for row in rows:
        emp_id = row["Employee #"].strip()
        name = f'{row["First Name"].strip()} {row["Last Name"].strip()}'
        position = row["Position"].strip()
        dept = department_for(position)
        location = row["Address"].strip() or "Quezon City"
        # The CSV's own Status column holds employment type (Regular/Probationary),
        # not the Active/On leave state this model needs. Everyone is Active.
        status = "Active"
        basic = money(row["Basic Salary"])
        rice = money(row["Rice Subsidy"])
        phone = money(row["Phone Allowance"])
        cloth = money(row["Clothing Allowance"])
        # "Immediate Supervisor" is written as "Last, First"; keep the link as an employee number
        # so the RBAC team scope is resolved from the file rather than guessed.
        raw_supervisor = row.get("Immediate Supervisor", "").strip()
        if raw_supervisor and raw_supervisor.upper() != "N/A":
            key = " ".join(raw_supervisor.lower().replace(",", " ").split())
            supervisor = supervisors.get(key)
            if supervisor is None:
                unresolved.add(raw_supervisor)
                supervisor = ""
        else:
            supervisor = ""
        lines.append(
            f'        addFixture(new Employee("{emp_id}", "{name}", "{dept}", '
            f'"{position}", "{location}", "{status}",\n'
            f'            new BigDecimal("{basic}"), new BigDecimal("{rice}"), '
            f'new BigDecimal("{phone}"), new BigDecimal("{cloth}"), '
            f'"{supervisor}"));'
        )

    if unresolved:
        raise SystemExit("Immediate Supervisor names not found in the file: " + ", ".join(sorted(unresolved)))

    source = OUT.read_text()
    header = source.split("public InMemoryEmployeeStore() {")[0]
    if "import java.math.BigDecimal;" not in header:
        header = header.replace("import java.util.ArrayList;",
                                "import java.math.BigDecimal;\nimport java.util.ArrayList;")
    tail = source.split("\n    }\n", 1)[1] if "\n    }\n" in source else ""

    body = (
        header
        + "public InMemoryEmployeeStore() {\n"
        + "\n".join(lines)
        + "\n    }\n"
        + tail
    )
    OUT.write_text(body)
    print(f"wrote {len(rows)} employees to {OUT}")


if __name__ == "__main__":
    main()
