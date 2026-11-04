
package motorphpayrollsystem;

public class PayrollBackendTest {

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println(" MOTORPH BACKEND TEST");
        System.out.println("==================================");

        try {
            // Create a sample full-time employee.
            FullTimeEmployee employee = new FullTimeEmployee(
                    10001,
                    "Santos",
                    "Juan",
                    "34-1234567-8",
                    "12-345678901-2",
                    "123-456-789",
                    "1234-5678-9012",
                    "Regular",
                    "Maria Reyes",
                    "IT Staff",
                    1500.00,
                    1000.00,
                    1000.00,
                    125.00,
                    20000.00
            );

            // Create the payroll processor.
            PayrollProcessor processor = new PayrollProcessor();

            // Process one semi-monthly payroll period.
            Payslip payslip = processor.processPayroll(
                    employee,
                    "November 1-15, 2026"
            );

            // Display the generated payslip.
            System.out.println(payslip.generatePayslip());

            // Check basic calculation relationships.
            double expectedRegularPay =
                    employee.getBasicSalary() / 2.0;

            double expectedGrossPay =
                    expectedRegularPay
                            + employee.getRiceSubsidy()
                            + employee.getPhoneAllowance()
                            + employee.getClothingAllowance();

            boolean regularPayPassed =
                    Math.abs(employee.calculatePay()
                            - expectedRegularPay) < 0.01;

            boolean grossPayPassed =
                    Math.abs(payslip.getGrossPay()
                            - expectedGrossPay) < 0.01;

            boolean netPayPassed =
                    Math.abs(payslip.getNetPay()
                            - Math.max(
                            0.0,
                            payslip.getGrossPay()
                                    - payslip.getTotalDeductions()
                    )) < 0.01;

            System.out.println();
            System.out.println("BACKEND TEST RESULTS");
            System.out.println("-------------------------------");
            System.out.println("Regular Pay: "
                    + (regularPayPassed ? "PASS" : "FAIL"));
            System.out.println("Gross Pay: "
                    + (grossPayPassed ? "PASS" : "FAIL"));
            System.out.println("Net Pay: "
                    + (netPayPassed ? "PASS" : "FAIL"));

            if (regularPayPassed && grossPayPassed && netPayPassed) {
                System.out.println();
                System.out.println("ALL BASIC BACKEND CHECKS PASSED!");
            } else {
                System.out.println();
                System.out.println("SOME BACKEND CHECKS FAILED.");
            }

        } catch (Exception e) {
            System.out.println("BACKEND TEST FAILED!");
            e.printStackTrace();
        }
    }
}