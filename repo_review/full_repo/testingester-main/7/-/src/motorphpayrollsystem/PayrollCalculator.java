
package motorphpayrollsystem;

public class PayrollCalculator {

    public PayrollCalculator() {
    }

    public double calculateRegularPay(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee cannot be null."
            );
        }

        double regularPay = employee.calculatePay();

        if (!Double.isFinite(regularPay) || regularPay < 0) {
            throw new IllegalArgumentException(
                    "Regular pay must be a valid nonnegative amount."
            );
        }

        return regularPay;
    }

    public double calculateOvertimePay(
            Employee employee,
            AttendanceRecord attendance,
            OvertimeRequest overtime) {

        if (employee == null || attendance == null) {
            throw new IllegalArgumentException(
                    "Employee and attendance are required."
            );
        }

        if (overtime == null || !overtime.isApproved()) {
            return 0.0;
        }

        double overtimeHours = attendance.calculateOvertimeHours();
        double approvedHours = overtime.getRequestedHours();

        double payableHours = Math.min(
                overtimeHours,
                approvedHours
        );

        if (payableHours <= 0) {
            return 0.0;
        }

        // Temporary base calculation.
        // Confirm the original CP2 overtime rate before final testing.
        return payableHours * employee.getHourlyRate();
    }

    public double calculateGrossPay(
            double regularPay,
            double overtimePay,
            Employee employee) {

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee cannot be null."
            );
        }

        if (!Double.isFinite(regularPay) || regularPay < 0 ||
                !Double.isFinite(overtimePay) || overtimePay < 0) {

            throw new IllegalArgumentException(
                    "Pay amounts must be valid and nonnegative."
            );
        }

        double allowances =
                employee.getRiceSubsidy()
                        + employee.getPhoneAllowance()
                        + employee.getClothingAllowance();

        return regularPay + overtimePay + allowances;
    }
}