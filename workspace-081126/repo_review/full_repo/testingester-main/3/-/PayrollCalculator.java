import java.util.Scanner;

public class PayrollCalculator {

    public static void calculatePayroll() {

        Scanner input = new Scanner(System.in);

        System.out.println("=== Payroll Calculator ===");

        System.out.print("Enter Hours Worked: ");
        double hoursWorked = input.nextDouble();

        System.out.print("Enter Hourly Rate: ");
        double hourlyRate = input.nextDouble();

        double grossSalary = hoursWorked * hourlyRate;

        double sss = grossSalary * 0.045;
        double philhealth = grossSalary * 0.02;
        double pagibig = 100;

        double totalDeductions = sss + philhealth + pagibig;
        double netSalary = grossSalary - totalDeductions;

        System.out.println("\n--- Payroll Details ---");
        System.out.println("Gross Salary: " + grossSalary);
        System.out.println("SSS Deduction: " + sss);
        System.out.println("PhilHealth Deduction: " + philhealth);
        System.out.println("Pag-IBIG Deduction: " + pagibig);
        System.out.println("Total Deductions: " + totalDeductions);
        System.out.println("Net Salary: " + netSalary);
    }
}
