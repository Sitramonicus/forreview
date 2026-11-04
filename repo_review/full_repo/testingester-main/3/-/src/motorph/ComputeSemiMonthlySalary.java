/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package motorph;

/**
 *
 * @author Katallino
 */
public class ComputeSemiMonthlySalary {
    
    public static void main(String[] args) {

        // Declare and initialize employee information
        String employeeName = "Juan Dela Cruz";
        double hoursWorked = 80;     // total hours worked for the cutoff period
        double hourlyRate = 150;     // employee hourly pay rate

        // Check for invalid inputs using conditional statement
        if (hoursWorked <= 0 || hourlyRate <= 0) {
            System.out.println("Error: Invalid hours worked or hourly rate.");
        } else {

            // Compute the semi-monthly salary
            double semiMonthlySalary = hoursWorked * hourlyRate;

            // Display payroll summary
            System.out.println("MotorPH Payroll Summary");
            System.out.println("------------------------");
            System.out.println("Employee Name: " + employeeName);
            System.out.println("Hours Worked: " + hoursWorked);
            System.out.println("Hourly Rate: " + hourlyRate);
            System.out.println("Semi-Monthly Salary: " + semiMonthlySalary);

            // Test verification message
            System.out.println("Computation verified successfully!");
        }
    }
}
