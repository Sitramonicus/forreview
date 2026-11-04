
package motorphpayrollsystem;

public class FullTimeEmployee extends Employee {

    private double basicSalary;

    public FullTimeEmployee(
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
            double basicSalary) {

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

        setBasicSalary(basicSalary);
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(double basicSalary) {
        if (!Double.isFinite(basicSalary) || basicSalary < 0) {
            throw new IllegalArgumentException(
                    "Basic salary cannot be negative.");
        }

        this.basicSalary = basicSalary;
    }

    @Override
    public double calculatePay() {
        return basicSalary / 2.0;
    }
}