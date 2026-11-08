
package motorphpayrollsystem;

public class Payslip {

    private final int employeeID;
    private final String payPeriod;
    private final double grossPay;
    private final double totalDeductions;

    public Payslip(
            int employeeID,
            String payPeriod,
            double grossPay,
            double totalDeductions) {

        if (employeeID <= 0) {
            throw new IllegalArgumentException(
                    "Employee ID must be positive.");
        }

        if (payPeriod == null || payPeriod.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Pay period cannot be empty.");
        }

        if (!Double.isFinite(grossPay) || grossPay < 0) {
            throw new IllegalArgumentException(
                    "Gross pay must be valid and nonnegative.");
        }

        if (!Double.isFinite(totalDeductions)
                || totalDeductions < 0) {
            throw new IllegalArgumentException(
                    "Total deductions must be valid and nonnegative.");
        }

        this.employeeID = employeeID;
        this.payPeriod = payPeriod.trim();
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
    }

    public int getEmployeeID() {
        return employeeID;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return Math.max(0, grossPay - totalDeductions);
    }

    public String generatePayslip() {
        return String.format(
                "========================================%n" +
                        "          MOTORPH PAYROLL SYSTEM%n" +
                        "                 PAYSLIP%n" +
                        "========================================%n" +
                        "Employee ID      : %d%n" +
                        "Pay Period       : %s%n" +
                        "----------------------------------------%n" +
                        "Gross Pay        : PHP %,.2f%n" +
                        "Total Deductions : PHP %,.2f%n" +
                        "----------------------------------------%n" +
                        "NET PAY          : PHP %,.2f%n" +
                        "========================================",
                employeeID,
                payPeriod,
                grossPay,
                totalDeductions,
                getNetPay()
        );
    }
}