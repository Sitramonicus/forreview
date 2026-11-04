package motorphemployee.oop;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PayrollCalculator - the orchestrator: attendance + employee in, one immutable PayrollRecord out
 * (MS1 Week 4/5 CRC: "PayrollCalculator (Service Class Using Abstraction)"; Method Dictionary rows
 * calculatePayroll / calculateGross / calculateNetPay).
 *
 * This class is the refactored home of the payroll logic that CP2 copy-pasted into BOTH
 * MainMenu (1,558 lines) and Dashboard (1,442 lines) - four calculate*() formulas, twice. After the
 * After the GUI rewiring there is exactly one implementation of each formula in the whole project.
 *
 * Uses abstraction, not concrete types: the statutory deductions are held as
 * List<GovernmentContributionCalculator>, so a new deduction is a new subclass plus one line in
 * the constructor - this class never changes and never asks "which calculator are you?".
 *
 * Two phases (Refactoring Plan design):
 *   phase 1 - SSS, PhilHealth, Pag-IBIG are computed on GROSS pay (one polymorphic loop)
 *   phase 2 - withholding tax is computed on the TAXABLE income (gross minus phase 1)
 *
 * Official matrices (2026-10-27): the four calculators follow the OFFICIAL government matrices, not
 * CP2, so a CP2 mismatch is expected and documented in oop/matrix-verification.md.
 */
public class PayrollCalculator {

    /** Standard MotorPH work day used by CP2 (MainMenu.java:1090: workedDays * hourlyRate * 8). */
    private static final double HOURS_PER_WORK_DAY = 8.0;

    /** Default payroll period length when the caller does not pass an explicit one. */
    private static final int DEFAULT_PERIOD_DAYS = 15;

    private static final ZoneId PAYROLL_ZONE = ZoneId.of("Asia/Manila");

    /** Contribution codes from the approved design; used to name the amounts, never to branch on type. */
    private static final String CODE_SSS = "SSS";
    private static final String CODE_PHILHEALTH = "PH";
    private static final String CODE_PAGIBIG = "PAGIBIG";

    private final List<GovernmentContributionCalculator> statutory;
    private final GovernmentContributionCalculator withholdingTax;

    /** Production default: the three statutory deductions plus BIR withholding tax. */
    public PayrollCalculator() {
        this(defaultStatutory(), new WithholdingTaxCalculator());
    }

    /**
     * Injectable composition - the constructor a test or a future payroll variant uses.
     * Because the field type is the abstract parent, the caller can swap any calculator.
     */
    public PayrollCalculator(List<GovernmentContributionCalculator> statutory,
                             GovernmentContributionCalculator withholdingTax) {
        if (statutory == null || statutory.isEmpty()) {
            throw new IllegalArgumentException("at least one statutory calculator is required");
        }
        for (GovernmentContributionCalculator calculator : statutory) {
            if (calculator == null) {
                throw new IllegalArgumentException("statutory calculator list cannot contain null");
            }
        }
        if (withholdingTax == null) {
            throw new IllegalArgumentException("withholdingTax calculator is required");
        }
        this.statutory = Collections.unmodifiableList(new ArrayList<>(statutory));
        this.withholdingTax = withholdingTax;
    }

    // ---------------------------------------------------------------- public API

    /**
     * Payroll for the most recent semi-monthly period (overload without dates).
     * Same computation as the 4-argument overload - only the period differs.
     */
    public PayrollRecord calculatePayroll(Employee employee, AttendanceLog attendance) {
        requireEmployee(employee);
        requireAttendance(attendance);

        LocalDate periodEndDay = LocalDate.now(PAYROLL_ZONE);
        LocalDate periodStartDay = periodEndDay.minusDays(DEFAULT_PERIOD_DAYS - 1L);
        return calculatePayroll(employee, attendance, periodStartDay.toString(), periodEndDay.toString());
    }

    /**
     * Payroll for an explicit period - the single public entry point of the whole computation.
     *
     * @throws IllegalArgumentException when the employee or attendance is missing, when the period
     *         is invalid, when the computed gross pay is not positive (Issue 1 fix), or
     *         when the statutory deductions would consume the entire gross pay.
     */
    public PayrollRecord calculatePayroll(Employee employee,
                                          AttendanceLog attendance,
                                          String periodStart,
                                          String periodEnd) {
        requireEmployee(employee);
        requireAttendance(attendance);
        LocalDate start = parsePeriodDate(periodStart, "periodStart");
        LocalDate end = parsePeriodDate(periodEnd, "periodEnd");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("periodEnd cannot be before periodStart: "
                    + periodStart + " -> " + periodEnd);
        }

        int employeeId = employee.getId();
        double hourlyRate = employee.getHourlyRate();
        int workedDays = attendance.getWorkedDays(employeeId, periodStart, periodEnd);
        double grossIncome = calculateGross(attendance, employeeId, hourlyRate, periodStart, periodEnd);

        // Issue 1: a non-positive gross pay must never reach the deduction tables,
        // otherwise the statutory minimums turn a 0.00 payslip into a negative one.
        if (!(grossIncome > 0)) {
            throw new IllegalArgumentException("cannot compute payroll for employee " + employeeId
                    + ": gross pay is " + grossIncome + " because " + workedDays
                    + " day(s) of attendance were found in " + periodStart + ".." + periodEnd);
        }

        // ---- phase 1: the three statutory deductions, one loop over the ABSTRACT type ----
        Map<String, Double> statutoryAmounts = new LinkedHashMap<>();
        double statutoryTotal = 0.0;
        for (GovernmentContributionCalculator calculator : statutory) {
            if (!calculator.isActive()) {
                continue;
            }
            double amount = calculator.calculate(grossIncome);
            statutoryAmounts.put(calculator.getContributionCode(), amount);
            statutoryTotal += amount;
        }

        double sss = amountOf(statutoryAmounts, CODE_SSS);
        double philhealth = amountOf(statutoryAmounts, CODE_PHILHEALTH);
        double pagibig = amountOf(statutoryAmounts, CODE_PAGIBIG);

        if (statutoryTotal >= grossIncome) {
            throw new IllegalArgumentException("statutory deductions (" + statutoryTotal
                    + ") would consume the whole gross pay (" + grossIncome
                    + ") for employee " + employeeId + " - no payslip can be produced");
        }

        // ---- phase 2: BIR withholding tax on the income already net of the three above ----
        double taxableIncome = grossIncome - statutoryTotal;
        double tax = withholdingTax.isActive() ? withholdingTax.calculate(taxableIncome) : 0.0;

        double totalDeductions = sss + philhealth + pagibig + tax;
        double netIncome = calculateNetPay(grossIncome, totalDeductions);

        ZonedDateTime issueDate = ZonedDateTime.now(PAYROLL_ZONE);
        return new PayrollRecord(
                employeeId,
                issueDate,
                start.atStartOfDay(PAYROLL_ZONE),
                end.atTime(23, 59).atZone(PAYROLL_ZONE),
                employee.getBasicSalary(),
                employee.getPhoneAllowance(),
                employee.getRiceSubsidy(),
                employee.getClothingAllowance(),
                grossIncome,
                employee.getGrossSemiMonthlyRate(),
                netIncome,
                sss,
                philhealth,
                pagibig,
                tax,
                employee.getAnnualLeaveCredits(),
                workedDays * HOURS_PER_WORK_DAY,
                hourlyRate,
                0L);                 // absences: no attendance.csv yet (open question), never invented
    }

    @Override
    public String toString() {
        return "PayrollCalculator{statutory=" + statutory + ", tax=" + withholdingTax + "}";
    }

    // ---------------------------------------------------------------- private computation

    /** gross = workedDays * hourlyRate * 8 (Method Dictionary row "calculateGross"). */
    private double calculateGross(AttendanceLog attendance,
                                  int employeeId,
                                  double hourlyRate,
                                  String periodStart,
                                  String periodEnd) {
        int workedDays = attendance.getWorkedDays(employeeId, periodStart, periodEnd);
        return workedDays * hourlyRate * HOURS_PER_WORK_DAY;
    }

    /** net = gross - all deductions (Method Dictionary row "calculateNetPay"). */
    private double calculateNetPay(double gross, double deductions) {
        if (Double.isNaN(gross) || Double.isNaN(deductions)) {
            throw new IllegalArgumentException("gross and deductions must be numbers");
        }
        if (deductions < 0) {
            throw new IllegalArgumentException("deductions cannot be negative: " + deductions);
        }
        return gross - deductions;
    }

    // ---------------------------------------------------------------- helpers

    private static List<GovernmentContributionCalculator> defaultStatutory() {
        List<GovernmentContributionCalculator> calculators = new ArrayList<>();
        calculators.add(new SSSCalculator());
        calculators.add(new PhilHealthCalculator());
        calculators.add(new PagIbigCalculator());
        return calculators;
    }

    private static double amountOf(Map<String, Double> amounts, String code) {
        Double amount = amounts.get(code);
        return amount == null ? 0.0 : amount;
    }

    private static void requireEmployee(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("employee is required");
        }
    }

    private static void requireAttendance(AttendanceLog attendance) {
        if (attendance == null) {
            throw new IllegalArgumentException("attendance is required");
        }
    }

    private static LocalDate parsePeriodDate(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must be yyyy-MM-dd: " + value);
        }
    }
}
