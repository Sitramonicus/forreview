#!/usr/bin/env python3
"""Move the single-package Java sources into the required folder layout and rewrite package/import lines."""
import re, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
MAIN = ROOT / "src/main/java/ph/motorph/web"
TEST = ROOT / "src/test/java/ph/motorph/web"

LAYOUT = {
 "o_oop": "Employee EmployeeStore MutableEmployeeStore InMemoryEmployeeStore FirestoreEmployeeStore EmployeeService EmployeeChangelog EmployeePhotos EmployeeScheduleStore InMemoryEmployeeScheduleStore PersonalDetails GovernmentDetails HrRecords PayrollProfile LeaveBalance LeavePolicy AttendanceRecord AttendanceStore MutableAttendanceStore InMemoryAttendanceStore FirestoreAttendanceStore RawAttendanceSummary FirestoreException FirestoreRestClient LocalStateFile DisbursementStore SavedPayslipStore WorkSchedule",
 "c_calc": "PayrollPolicy StatutoryRates StatutoryCalculator2026 StatutoryBreakdown CutoffPay CutoffPayrollService MonthPayroll PayrollAdjustments HolidayCalendar2026 AttendanceMetrics SalaryHistory PayslipPdf AttendanceService ClockService",
 "t_tkt": "WorkflowService WorkflowStore InMemoryWorkflowStore FirestoreWorkflowStore DemoLeaveRequest DemoOvertimeRequest DemoTicket DemoSeedService AdjustmentForms PayChangeRequests RateProposals PayslipCorrections Notifications PayslipOutbox",
 "l_log": "AuditLog",
 "sp_secpol": "AccessPolicy AccountSecurity TwoFactor",
 "svc": "AppServer",
}
CLS = {c: p for p, names in LAYOUT.items() for c in names.split()}

def run(base):
    originals = {p.stem: p for p in base.glob("*.java")}
    missing = [c for c in CLS if c not in originals]
    if missing: print("missing:", missing)
    texts = {}
    for name, pkg in CLS.items():
        if name not in originals: continue
        t = originals[name].read_text()
        t = re.sub(r"^package [\w.]+;", f"package ph.motorph.{pkg};", t, count=1, flags=re.M)
        texts[name] = t
    # cross-package imports
    for name, t in list(texts.items()):
        pkg = CLS[name]
        needed = []
        for other, opkg in CLS.items():
            if other == name or opkg == pkg or other not in texts: continue
            if re.search(rf"(?<![\w.\"]){re.escape(other)}\b", t):
                needed.append(f"import ph.motorph.{opkg}.{other};")
        if needed:
            lines = t.split("\n")
            idx = next((i for i, l in enumerate(lines) if l.startswith("import ")), None)
            if idx is None:
                idx = next(i for i, l in enumerate(lines) if l.startswith("package ")) + 1
                lines[idx:idx] = [""] + sorted(needed)
            else:
                lines[idx:idx] = sorted(needed)
            texts[name] = "\n".join(lines)
    for name, t in texts.items():
        d = base / CLS[name]; d.mkdir(exist_ok=True)
        (d / f"{name}.java").write_text(t)
        originals[name].unlink()
    print(f"{base.name}: {len(texts)} moved")

run(MAIN); run(TEST)
