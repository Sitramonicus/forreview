package motorph.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Computed payroll result for one employee and one cutoff.
 * <p>
 * Immutable: all fields are final, there are no setters, and the deduction
 * breakdown is copied in and copied out. Total deductions is always the sum
 * of the breakdown, so the itemized lines and the total can never disagree.
 * Amounts are kept at full precision and rounded to 2 decimals (HALF_UP) when
 * displayed or saved, the same point where CP2 rounded.
 * </p>
 */
public final class Payslip {

    private final String employeeId;
    private final String employeeName;
    private final String employmentType;
    private final String cutoffLabel;
    private final BigDecimal hourlyRate;
    private final double hoursWorked;
    private final BigDecimal grossPay;
    private final Map<String, BigDecimal> deductionBreakdown;
    private final BigDecimal totalDeductions;
    private final BigDecimal allowances;
    private final BigDecimal netPay;

    /**
     * Creates a payslip. Copies the employee's details so later edits to the
     * Employee object do not change an issued payslip.
     *
     * @param employee           the employee paid
     * @param cutoffLabel        for example "June 16-30, 2024"
     * @param hoursWorked        hours worked in this cutoff
     * @param grossPay           gross pay for this cutoff
     * @param deductionBreakdown deduction name to amount, in payslip order
     * @param allowances         allowances listed on this payslip (not added to net pay)
     * @param netPay             net pay for this cutoff
     */
    public Payslip(Employee employee, String cutoffLabel, double hoursWorked, BigDecimal grossPay,
                   Map<String, BigDecimal> deductionBreakdown, BigDecimal allowances, BigDecimal netPay) {
        if (employee == null || cutoffLabel == null || grossPay == null || deductionBreakdown == null
                || allowances == null || netPay == null) {
            throw new IllegalArgumentException("All payslip values are required.");
        }
        this.employeeId = employee.getEmployeeId();
        this.employeeName = employee.getFullName();
        this.employmentType = employee.getEmploymentType();
        this.hourlyRate = employee.getHourlyRate();
        this.cutoffLabel = cutoffLabel;
        this.hoursWorked = hoursWorked;
        this.grossPay = grossPay;
        this.deductionBreakdown = Collections.unmodifiableMap(new LinkedHashMap<>(deductionBreakdown));
        this.totalDeductions = this.deductionBreakdown.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        this.allowances = allowances;
        this.netPay = netPay;
    }

    // === GETTERS (no setters: values are final after creation) ===

    public String getEmployeeId()         { return employeeId; }
    public String getEmployeeName()       { return employeeName; }
    public String getEmploymentType()     { return employmentType; }
    public String getCutoffLabel()        { return cutoffLabel; }
    public BigDecimal getHourlyRate()     { return hourlyRate; }
    public double getHoursWorked()        { return hoursWorked; }
    public BigDecimal getGrossPay()       { return grossPay; }
    public BigDecimal getTotalDeductions(){ return totalDeductions; }
    public BigDecimal getAllowances()     { return allowances; }
    public BigDecimal getNetPay()         { return netPay; }

    /** Returns one deduction by name, or 0 if this payslip has no such deduction. */
    public BigDecimal getDeductionAmount(String deductionName) {
        return deductionBreakdown.getOrDefault(deductionName, BigDecimal.ZERO);
    }

    /** Returns a copy of the deduction breakdown, so the payslip itself cannot be edited. */
    public Map<String, BigDecimal> getDeductionBreakdown() {
        return new LinkedHashMap<>(deductionBreakdown);
    }

    /**
     * Formats the payslip as one payroll_results.csv line: Employee ID, Name,
     * Hourly Rate, Hours Worked, Gross Pay, each deduction in breakdown order,
     * Total Deductions, Net Pay. Amounts use 2 decimals, rounded HALF_UP.
     */
    public String toCSVRow() {
        StringBuilder row = new StringBuilder();
        row.append(employeeId).append(',').append(quoteIfNeeded(employeeName));
        row.append(',').append(formatAmount(hourlyRate));
        row.append(',').append(String.format(Locale.US, "%.2f", hoursWorked));
        row.append(',').append(formatAmount(grossPay));
        for (BigDecimal amount : deductionBreakdown.values()) {
            row.append(',').append(formatAmount(amount));
        }
        row.append(',').append(formatAmount(totalDeductions));
        row.append(',').append(formatAmount(netPay));
        return row.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        return String.format(Locale.US, "%.2f", amount);   // Formatter rounds BigDecimal HALF_UP
    }

    private static String quoteIfNeeded(String text) {
        if (text.contains(",") || text.contains("\"")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
