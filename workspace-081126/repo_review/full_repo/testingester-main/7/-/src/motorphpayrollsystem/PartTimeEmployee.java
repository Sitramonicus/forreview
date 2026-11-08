package motorphpayrollsystem;

public class PartTimeEmployee extends Employee {

    private double hoursWorked;

    public PartTimeEmployee(
            int employeeID,
            String lastName,
            String firstName,
            String sssNumber,
            String philHealthNumber,
            String tinNumber,
            String pagIbigNumber,
            String status,
            String immediateSupervisor,
            String position,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance,
            double hourlyRate,
            double hoursWorked) {

        super(
                employeeID,
                lastName,
                firstName,
                sssNumber,
                philHealthNumber,
                tinNumber,
                pagIbigNumber,
                status,
                immediateSupervisor,
                position,
                riceSubsidy,
                phoneAllowance,
                clothingAllowance,
                hourlyRate
        );

        setHoursWorked(hoursWorked);
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        if (!Double.isFinite(hoursWorked) || hoursWorked < 0) {
            throw new IllegalArgumentException(
                    "Hours worked cannot be negative."
            );
        }

        this.hoursWorked = hoursWorked;
    }

    @Override
    public double calculatePay() {
        return getHourlyRate() * hoursWorked;
    }
}