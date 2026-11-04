package motorph.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import motorph.deduction.Deduction;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.Payslip;

/**
 * Runs the payroll workflow: hours -> gross pay -> deductions -> net pay -> Payslip.
 * <p>
 * Works only with parent types (Employee, Deduction), so there are no checks on
 * employee status or deduction type. The deductions are passed into the
 * constructor: a list of contributions computed on monthly gross (SSS,
 * PhilHealth, Pag-IBIG) and a separate withholding tax computed on taxable
 * income. Adding a new government contribution only needs a new Deduction
 * subclass in that list.
 * </p>
 * <p>
 * Cutoff rule (from CP2): the 1st cutoff (days 1-15) has no deductions and net
 * pay equals gross pay. The 2nd cutoff (day 16 to month end) takes the whole
 * month's deductions once, computed on the combined gross of both cutoffs.
 * </p>
 */
public class PayrollProcessor {

    // === SUMMARY KEYS (computePayrollSummary) ===
    public static final String SUMMARY_TOTAL_EMPLOYEES  = "Total Employees";
    public static final String SUMMARY_TOTAL_GROSS      = "Total Gross Pay";
    public static final String SUMMARY_TOTAL_DEDUCTIONS = "Total Deductions";
    public static final String SUMMARY_TOTAL_NET        = "Total Net Pay";
    public static final String SUMMARY_AVERAGE_NET      = "Average Net Pay";

    // Cutoff labels come from AttendanceRecord.getCutoffLabel(), e.g. "June 16-30, 2024"
    private static final String SECOND_CUTOFF_MARKER  = " 16-";
    private static final String SECOND_CUTOFF_PATTERN = " 16-\\d{2},";
    private static final String FIRST_CUTOFF_DAYS     = " 1-15,";

    private final List<Deduction> contributions;
    private final Deduction withholdingTax;

    /**
     * @param contributions  deductions computed on monthly gross, in payslip order
     * @param withholdingTax deduction computed last, on taxable income
     */
    public PayrollProcessor(List<Deduction> contributions, Deduction withholdingTax) {
        if (contributions == null || withholdingTax == null) {
            throw new IllegalArgumentException("Contributions and withholding tax are required.");
        }
        for (Deduction contribution : contributions) {
            if (contribution == null) {
                throw new IllegalArgumentException("Contribution list must not contain empty entries.");
            }
        }
        this.contributions = List.copyOf(contributions);
        this.withholdingTax = withholdingTax;
    }

    /** Returns the deduction names in payslip order (contributions, then tax). */
    public List<String> getDeductionNames() {
        List<String> names = new ArrayList<>();
        for (Deduction contribution : contributions) {
            names.add(contribution.getName());
        }
        names.add(withholdingTax.getName());
        return names;
    }

    // === CUTOFFS AND HOURS ===

    /**
     * Returns every cutoff label found in the attendance records, oldest first.
     * Used to fill the cutoff selection list.
     */
    public List<String> getAvailableCutoffs(Map<String, List<AttendanceRecord>> attendanceByEmployee) {
        Map<LocalDate, String> labelsByDate = new TreeMap<>();
        for (List<AttendanceRecord> records : attendanceByEmployee.values()) {
            for (AttendanceRecord record : records) {
                labelsByDate.putIfAbsent(record.getDate(), record.getCutoffLabel());
            }
        }
        Set<String> cutoffs = new LinkedHashSet<>(labelsByDate.values());
        return new ArrayList<>(cutoffs);
    }

    /**
     * Sums the hours worked in one cutoff.
     *
     * @param records     one employee's attendance records
     * @param cutoffLabel the cutoff to total, for example "June 1-15, 2024"
     * @return total hours, or 0 if no records match
     */
    public double computeHoursForCutoff(List<AttendanceRecord> records, String cutoffLabel) {
        long minutes = 0;
        if (records != null) {
            for (AttendanceRecord record : records) {
                if (record.getCutoffLabel().equals(cutoffLabel)) {
                    minutes += record.computeMinutesWorked();
                }
            }
        }
        return minutes / 60.0;
    }

    /**
     * Returns the employees with no hours in the cutoff, so the GUI can warn once
     * before they are computed with 0 hours (CP2 behavior).
     */
    public List<Employee> findEmployeesWithoutAttendance(List<Employee> employees,
            Map<String, List<AttendanceRecord>> attendanceByEmployee, String cutoffLabel) {
        List<Employee> missing = new ArrayList<>();
        for (Employee employee : employees) {
            List<AttendanceRecord> records = attendanceByEmployee.get(employee.getEmployeeId());
            if (computeHoursForCutoff(records, cutoffLabel) <= 0) {
                missing.add(employee);
            }
        }
        return missing;
    }

    // === DEDUCTIONS AND NET PAY ===

    /**
     * Computes each deduction for one month, in order: every contribution on the
     * monthly gross, then withholding tax on taxable income (monthly gross minus
     * the contributions, floored at 0). No type checks: each Deduction runs its
     * own formula.
     *
     * @param monthlyGross combined gross of the 1st and 2nd cutoff
     * @return deduction name to amount, in payslip order
     */
    public Map<String, BigDecimal> computeDeductionBreakdown(BigDecimal monthlyGross) {
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        BigDecimal taxableIncome = monthlyGross;
        for (Deduction contribution : contributions) {
            BigDecimal amount = contribution.computeDeduction(monthlyGross);
            breakdown.put(contribution.getName(), amount);
            taxableIncome = taxableIncome.subtract(amount);
        }
        breakdown.put(withholdingTax.getName(), withholdingTax.computeDeduction(taxableIncome.max(BigDecimal.ZERO)));
        return breakdown;
    }

    /**
     * Totals all government deductions for one month: the sum of
     * computeDeductionBreakdown(), so the total always matches the itemized lines.
     */
    public BigDecimal computeTotalDeductions(BigDecimal monthlyGross) {
        return sum(computeDeductionBreakdown(monthlyGross));
    }

    /** Net pay = cutoff gross - deductions, never below 0. */
    public BigDecimal computeNetPay(BigDecimal cutoffGross, BigDecimal totalDeductions) {
        return cutoffGross.subtract(totalDeductions).max(BigDecimal.ZERO);
    }

    // === PAYSLIPS ===

    /**
     * Builds the payslip for one employee and one cutoff ("One Employee" mode).
     * Accepts any Employee subtype; allowances come from the subclass's own
     * computeAllowances().
     *
     * @param employee    the employee to pay
     * @param records     that employee's attendance records (may be empty)
     * @param cutoffLabel the cutoff to pay
     */
    public Payslip generatePayslip(Employee employee, List<AttendanceRecord> records, String cutoffLabel) {
        if (employee == null || cutoffLabel == null || cutoffLabel.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee and cutoff are required.");
        }
        double hoursWorked = computeHoursForCutoff(records, cutoffLabel);
        BigDecimal grossPay = employee.computeGrossPay(hoursWorked);

        Map<String, BigDecimal> breakdown;
        BigDecimal allowances;
        if (isSecondCutoff(cutoffLabel)) {
            // Monthly items are settled once, here: deductions on the combined gross of both cutoffs,
            // and the month's allowances (listed only; not added to net pay, as in CP2).
            double firstCutoffHours = computeHoursForCutoff(records, getFirstCutoffLabel(cutoffLabel));
            BigDecimal monthlyGross = employee.computeGrossPay(firstCutoffHours + hoursWorked);
            breakdown = computeDeductionBreakdown(monthlyGross);
            allowances = employee.computeAllowances();
        } else {
            breakdown = zeroBreakdown();
            allowances = BigDecimal.ZERO;
        }
        BigDecimal netPay = computeNetPay(grossPay, sum(breakdown));
        return new Payslip(employee, cutoffLabel, hoursWorked, grossPay, breakdown, allowances, netPay);
    }

    /**
     * Runs payroll for every employee in one cutoff ("All Employees" mode).
     * Same steps for every Employee subtype.
     */
    public List<Payslip> processPayroll(List<Employee> employees,
            Map<String, List<AttendanceRecord>> attendanceByEmployee, String cutoffLabel) {
        List<Payslip> payslips = new ArrayList<>();
        for (Employee employee : employees) {
            payslips.add(generatePayslip(employee, attendanceByEmployee.get(employee.getEmployeeId()), cutoffLabel));
        }
        return payslips;
    }

    /**
     * Totals a list of payslips: employee count, total gross, total deductions,
     * total net, and average net. Uses full-precision amounts (as CP2 did), so
     * the totals are rounded only when displayed.
     *
     * @throws IllegalArgumentException if the list is empty
     */
    public Map<String, BigDecimal> computePayrollSummary(List<Payslip> payslips) {
        if (payslips == null || payslips.isEmpty()) {
            throw new IllegalArgumentException("There are no payslips to summarize.");
        }
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        for (Payslip payslip : payslips) {
            totalGross = totalGross.add(payslip.getGrossPay());
            totalDeductions = totalDeductions.add(payslip.getTotalDeductions());
            totalNet = totalNet.add(payslip.getNetPay());
        }
        BigDecimal count = BigDecimal.valueOf(payslips.size());

        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        summary.put(SUMMARY_TOTAL_EMPLOYEES, count);
        summary.put(SUMMARY_TOTAL_GROSS, totalGross);
        summary.put(SUMMARY_TOTAL_DEDUCTIONS, totalDeductions);
        summary.put(SUMMARY_TOTAL_NET, totalNet);
        summary.put(SUMMARY_AVERAGE_NET, totalNet.divide(count, MathContext.DECIMAL128));
        return summary;
    }

    // === HELPERS ===

    private Map<String, BigDecimal> zeroBreakdown() {
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        for (String name : getDeductionNames()) {
            breakdown.put(name, BigDecimal.ZERO);
        }
        return breakdown;
    }

    private static BigDecimal sum(Map<String, BigDecimal> amounts) {
        return amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean isSecondCutoff(String cutoffLabel) {
        return cutoffLabel.contains(SECOND_CUTOFF_MARKER);
    }

    /** "June 16-30, 2024" becomes "June 1-15, 2024". */
    private static String getFirstCutoffLabel(String secondCutoffLabel) {
        return secondCutoffLabel.replaceFirst(SECOND_CUTOFF_PATTERN, FIRST_CUTOFF_DAYS);
    }
}
