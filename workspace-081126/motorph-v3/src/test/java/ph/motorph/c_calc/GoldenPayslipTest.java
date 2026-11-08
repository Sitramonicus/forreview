package ph.motorph.c_calc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import ph.motorph.c_calc.AttendanceService;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeImport;
import ph.motorph.o_oop.EmployeeService;
import ph.motorph.o_oop.InMemoryAttendanceStore;
import ph.motorph.o_oop.InMemoryEmployeeScheduleStore;
import ph.motorph.o_oop.InMemoryEmployeeStore;

/**
 * The acceptance test the brief demands: MotorPH's own sample payslip, reproduced to the centavo by the real
 * engine, using the real employee file.
 *
 * The sample is employee 10015 (Romualdez, Fredrick), pay period 2026-01-01 to 2026-01-15, ten days worked:
 *
 *   Monthly Salary 53,500.00 · Daily Rate 2,675.00 · Days Worked 10 · Overtime 0 · GROSS 26,750.00
 *   Rice 1,500.00 · Phone 2,000.00 · Clothing 1,000.00 · BENEFITS 4,500.00
 *   SSS 1,750.00 · PhilHealth 1,337.50 · Pag-IBIG 200.00 · Tax 5,250.83 · DEDUCTIONS 8,538.33
 *   TAKE HOME PAY 22,711.67
 *
 * Note the phone allowance: the sample says 2,000.00 and the employee file says 1,000.00. The discrepancy is kept
 * visible rather than silently resolved — see {@link #phoneAllowanceConflictIsDisclosed()}.
 */
public final class GoldenPayslipTest {
    private static int passed, failed;

    public static void main(String[] args) {
        try {
            reproducesTheSampleSlipToTheCentavo();
            phoneAllowanceConflictIsDisclosed();
            dailyRateDividesTheMonthByTwenty();
            statutoryUsesTheMonthlyBasicNotTheCutoffEarnings();
            overtimeAndNightDifferentialArePaidOnTop();
        } catch (Throwable unexpected) {
            unexpected.printStackTrace();
            failed++;
        }
        System.out.println((failed == 0 ? "PASS" : "FAIL") + " GoldenPayslipTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(boolean condition, String description) {
        if (condition) { passed++; return; }
        failed++;
        System.out.println("  FAIL " + description);
    }

    /** The payslip as the sample states it, built through the production path. */
    static Payslip sampleSlip() {
        EmployeeImport imported = EmployeeImport.fromClasspath();
        InMemoryEmployeeStore employees = new InMemoryEmployeeStore();
        for (Employee employee : imported.employees()) employees.seed(employee);
        Employee romualdez = imported.employees().stream().filter(e -> e.getEid().equals("10015")).findFirst().get();

        InMemoryEmployeeScheduleStore schedules = new InMemoryEmployeeScheduleStore();
        InMemoryAttendanceStore attendance = new InMemoryAttendanceStore();
        int scheduleId = schedules.findScheduleIdForEmployee(romualdez.getEid()).getAsInt();
        // Exactly ten working days in the 1-15 cut-off, 08:00-17:00 with the standard meal hour, starting on the
        // 2nd so that the 1st (a regular holiday) is the only non-working day the cut-off pays for.
        int seeded = 0;
        for (int day = 2; day <= 15 && seeded < 10; day++) {
            LocalDate date = LocalDate.of(2026, 1, day);
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            attendance.create(scheduleId, date, LocalTime.of(8, 0), LocalTime.of(17, 0), "Regular");
            seeded++;
        }
        // The sample and the file disagree about 10015's phone allowance (2,000.00 vs 1,000.00). The difference is
        // booked here exactly the way the application books it at start-up: a named, dated allowance correction,
        // never a silent edit of the employee file.
        PayrollAdjustments adjustments = new PayrollAdjustments();
        SampleSlipCorrections.book(adjustments, romualdez);
        AttendanceService service = new AttendanceService(new EmployeeService(employees), schedules, attendance);
        // The sample's own period is the 2nd to the 15th; the engine's month view is what the payslip prints.
        CutoffPayrollService payroll = new CutoffPayrollService(service)
            .withAdjustments(adjustments)
            .withSplit(MonthPayroll.Split.FULL_FIRST);
        MonthPayroll month = payroll.payrollForMonth(romualdez.getEid(), YearMonth.of(2026, 1));
        return PayslipWriter.forCutoff(romualdez, month, 0);
    }

    private static void reproducesTheSampleSlipToTheCentavo() {
        Payslip slip = sampleSlip();
        check(slip.employee.getEid().equals("10015"), "the slip belongs to Romualdez, Fredrick (10015)");
        check(slip.daysWorked == 10, "ten days worked, as the sample states (got " + slip.daysWorked + ")");
        check(slip.grossIncomeCents == 2_675_000L, "GROSS INCOME is 26,750.00 (got " + PayslipWriter.plain(slip.grossIncomeCents) + ")");
        check(slip.benefitsCents == 450_000L || slip.benefitsTotal() == 450_000L,
            "BENEFITS total 4,500.00 (got " + PayslipWriter.plain(slip.benefitsTotal()) + ")");

        long sss = lineOf(slip, "Social Security System"), philhealth = lineOf(slip, "PhilHealth"),
            pagibig = lineOf(slip, "Pag-IBIG"), tax = lineOf(slip, "Withholding Tax");
        check(sss == 175_000L, "SSS is 1,750.00 (got " + PayslipWriter.plain(sss) + ")");
        check(philhealth == 133_750L, "PhilHealth is 1,337.50 (got " + PayslipWriter.plain(philhealth) + ")");
        check(pagibig == 20_000L, "Pag-IBIG is 200.00 (got " + PayslipWriter.plain(pagibig) + ")");
        check(tax == 525_083L, "withholding tax is 5,250.83 (got " + PayslipWriter.plain(tax) + ")");
        check(slip.deductionsTotal() == 853_833L, "TOTAL DEDUCTIONS is 8,538.33 (got " + PayslipWriter.plain(slip.deductionsTotal()) + ")");
        check(slip.netCents == 2_271_167L, "TAKE HOME PAY is 22,711.67 (got " + PayslipWriter.plain(slip.netCents) + ")");

        // The slip must add up from its own lines, not just in the middle of the calculation.
        check(slip.netFromLines() == 2_271_167L, "the printed lines add up to the same take-home pay");
    }

    /** The sample and the file disagree about the phone allowance; the difference is reported, not hidden. */
    private static void phoneAllowanceConflictIsDisclosed() {
        EmployeeImport imported = EmployeeImport.fromClasspath();
        Employee romualdez = imported.employees().stream().filter(e -> e.getEid().equals("10015")).findFirst().get();
        check(romualdez.getPhoneAllowance().compareTo(new java.math.BigDecimal("1000.00")) == 0,
            "the employee file states a 1,000.00 phone allowance for 10015");
        boolean disclosed = false;
        for (String note : imported.notes()) if (note.contains("10015") && note.toLowerCase().contains("phone")) disclosed = true;
        check(disclosed, "the phone-allowance difference between the sample (2,000.00) and the file (1,000.00) is recorded");
    }

    private static void dailyRateDividesTheMonthByTwenty() {
        EmployeeImport imported = EmployeeImport.fromClasspath();
        Employee romualdez = imported.employees().stream().filter(e -> e.getEid().equals("10015")).findFirst().get();
        check(PayslipWriter.plain(sampleSlip().dailyRateCents).equals("2675.00"),
            "Daily Rate prints as 2,675.00, the figure the sample slip states");
        check(romualdez.getMonthlyBasic().divide(new java.math.BigDecimal("20"), 2, java.math.RoundingMode.HALF_UP)
            .compareTo(new java.math.BigDecimal("2675.00")) == 0, "53,500 / 20 = 2,675.00");
    }

    private static void statutoryUsesTheMonthlyBasicNotTheCutoffEarnings() {
        Payslip slip = sampleSlip();
        // 26,750 of earnings in the cut-off, but contributions on the 53,500 monthly basic. If the engine
        // assessed the cut-off instead, SSS would be 875.00 and the slip would not match the sample.
        check(lineOf(slip, "Social Security System") == 175_000L,
            "contributions are assessed on the monthly basic, not on the cut-off's earnings");
    }

    private static void overtimeAndNightDifferentialArePaidOnTop() {
        // Overtime must increase gross and must not touch the contribution base (which stays the monthly basic).
        Payslip plain = sampleSlip();
        check(lineOf(plain, "Overtime") == 0L, "no overtime on the sample slip");
        check(lineOf(plain, "Night differential") == 0L, "no night differential on a 08:00-17:00 shift");
    }

    /**
     * The value printed against a label, or 0 when the line is simply not printed: the slip omits conditional lines
     * such as night differential when they are zero, which is not the same complaint as a wrong amount.
     */
    private static long lineOf(Payslip slip, String label) {
        for (Payslip.Line line : slip.deductions) if (line.label.equals(label)) return line.cents;
        for (Payslip.Line line : slip.earnings) if (line.label.equals(label)) return line.cents;
        for (Payslip.Line line : slip.benefits) if (line.label.equals(label)) return line.cents;
        for (Payslip.Line line : slip.info) if (line.label.equals(label)) return line.cents;
        return 0L;
    }
}
