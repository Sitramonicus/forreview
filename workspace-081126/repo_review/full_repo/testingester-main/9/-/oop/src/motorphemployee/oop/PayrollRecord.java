package motorphemployee.oop;

import java.time.ZonedDateTime;

/**
 * PayrollRecord - immutable value object holding one complete payroll result
 * (MS1 Week 2 CRC: "PayrollRecord"; Method Dictionary: constructor + getters, NO setters).
 *
 * Built once by PayrollCalculator and never changed afterwards. All numeric fields are
 * validated at construction, so an impossible payslip (negative net pay, acme deductions)
 * cannot exist. Every field is final and every return value is an immutable type.
 */
public final class PayrollRecord {

    private final long employeeId;
    private final ZonedDateTime issueDate;
    private final ZonedDateTime periodStart;
    private final ZonedDateTime periodEnd;
    private final double basicSalaryBasis;
    private final double phoneAllowance;
    private final double riceSubsidy;
    private final double clothingAllowance;
    private final double grossIncome;
    private final double grossSemiMonthlyIncome;
    private final double netIncome;
    private final double sssContribution;
    private final double philhealthContribution;
    private final double pagibigContribution;
    private final double withholdingTax;
    private final long totalLeaves;
    private final double hoursWorked;
    private final double hourlyRate;
    private final long absences;

    public PayrollRecord(long employeeId,
                         ZonedDateTime issueDate,
                         ZonedDateTime periodStart,
                         ZonedDateTime periodEnd,
                         double basicSalaryBasis,
                         double phoneAllowance,
                         double riceSubsidy,
                         double clothingAllowance,
                         double grossIncome,
                         double grossSemiMonthlyIncome,
                         double netIncome,
                         double sssContribution,
                         double philhealthContribution,
                         double pagibigContribution,
                         double withholdingTax,
                         long totalLeaves,
                         double hoursWorked,
                         double hourlyRate,
                         long absences) {

        if (employeeId <= 0) {
            throw new IllegalArgumentException("employeeId must be positive: " + employeeId);
        }
        if (issueDate == null || periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("issueDate, periodStart and periodEnd are required");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("periodEnd cannot be before periodStart");
        }

        this.employeeId = employeeId;
        this.issueDate = issueDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.basicSalaryBasis = nonNegative(basicSalaryBasis, "basicSalaryBasis");
        this.phoneAllowance = nonNegative(phoneAllowance, "phoneAllowance");
        this.riceSubsidy = nonNegative(riceSubsidy, "riceSubsidy");
        this.clothingAllowance = nonNegative(clothingAllowance, "clothingAllowance");
        this.grossIncome = nonNegative(grossIncome, "grossIncome");
        this.grossSemiMonthlyIncome = nonNegative(grossSemiMonthlyIncome, "grossSemiMonthlyIncome");
        this.netIncome = nonNegative(netIncome, "netIncome");
        this.sssContribution = nonNegative(sssContribution, "sssContribution");
        this.philhealthContribution = nonNegative(philhealthContribution, "philhealthContribution");
        this.pagibigContribution = nonNegative(pagibigContribution, "pagibigContribution");
        this.withholdingTax = nonNegative(withholdingTax, "withholdingTax");
        this.totalLeaves = nonNegative(totalLeaves, "totalLeaves");
        this.hoursWorked = nonNegative(hoursWorked, "hoursWorked");
        this.hourlyRate = nonNegative(hourlyRate, "hourlyRate");
        this.absences = nonNegative(absences, "absences");

        double declaredDeductions = this.sssContribution + this.philhealthContribution
                + this.pagibigContribution + this.withholdingTax;
        double impliedNet = this.grossIncome - declaredDeductions;
        if (Math.abs(impliedNet - this.netIncome) > 0.01) {
            throw new IllegalArgumentException("netIncome " + netIncome
                    + " does not match grossIncome - deductions (" + impliedNet + ")");
        }
    }

    public long getEmployeeId() {
        return employeeId;
    }

    public ZonedDateTime getIssueDate() {
        return issueDate;
    }

    public ZonedDateTime getPeriodStart() {
        return periodStart;
    }

    public ZonedDateTime getPeriodEnd() {
        return periodEnd;
    }

    public double getBasicSalaryBasis() {
        return basicSalaryBasis;
    }

    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    public double getGrossIncome() {
        return grossIncome;
    }

    public double getGrossSemiMonthlyIncome() {
        return grossSemiMonthlyIncome;
    }

    public double getNetIncome() {
        return netIncome;
    }

    public double getSSSContribution() {
        return sssContribution;
    }

    public double getPhilHealthContribution() {
        return philhealthContribution;
    }

    public double getPagIbigContribution() {
        return pagibigContribution;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public long getTotalLeaves() {
        return totalLeaves;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public long getAbsences() {
        return absences;
    }

    public double getTotalDeductions() {
        return sssContribution + philhealthContribution + pagibigContribution + withholdingTax;
    }

    @Override
    public String toString() {
        return String.format(
                "PayrollRecord{employee=%d, period=%s..%s, gross=%.2f, sss=%.2f, philhealth=%.2f, "
                        + "pagibig=%.2f, tax=%.2f, net=%.2f}",
                employeeId, periodStart.toLocalDate(), periodEnd.toLocalDate(), grossIncome,
                sssContribution, philhealthContribution, pagibigContribution, withholdingTax, netIncome);
    }

    private static double nonNegative(double value, String field) {
        if (Double.isNaN(value) || value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative: " + value);
        }
        return value;
    }

    private static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative: " + value);
        }
        return value;
    }
}
