/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package motorph;

/**
 *
 * @author Katallino
 */
public class MotorPH_ApplyDeductions {
    
     // Method to compute SSS deduction
    public static double computeSSS(double grossSalary) {
        return grossSalary * 0.045; // Example: 4.5%
    }

    // Method to compute PhilHealth deduction
    public static double computePhilHealth(double grossSalary) {
        return grossSalary * 0.02; // Example: 2%
    }

    // Method to compute Pag-IBIG deduction
    public static double computePagIbig(double grossSalary) {
        return grossSalary * 0.01; // Example: 1%
    }

    // Method to compute Income Tax
    public static double computeIncomeTax(double grossSalary) {
        return grossSalary * 0.10; // Example: 10%
    }

    // Method to compute Net Pay
    public static double computeNetPay(double grossSalary) {

        // Call all deduction methods
        double sss = computeSSS(grossSalary);
        double philHealth = computePhilHealth(grossSalary);
        double pagIbig = computePagIbig(grossSalary);
        double tax = computeIncomeTax(grossSalary);

        double totalDeductions = sss + philHealth + pagIbig + tax;

        return grossSalary - totalDeductions;
    }

    public static void main(String[] args) {

        // Example gross salary from Task 8
        double grossSalary = 30000;

        // Validate salary input
        if (grossSalary <= 0) {
            System.out.println("Invalid salary amount.");
            return;
        }

        // Compute deductions
        double sss = computeSSS(grossSalary);
        double philHealth = computePhilHealth(grossSalary);
        double pagIbig = computePagIbig(grossSalary);
        double tax = computeIncomeTax(grossSalary);

        double totalDeductions = sss + philHealth + pagIbig + tax;
        double netPay = computeNetPay(grossSalary);

        // Display payroll summary
        System.out.println("====== MotorPH Payroll Summary ======");
        System.out.println("Gross Salary: " + grossSalary);
        System.out.println("SSS Deduction: " + sss);
        System.out.println("PhilHealth Deduction: " + philHealth);
        System.out.println("Pag-IBIG Deduction: " + pagIbig);
        System.out.println("Income Tax: " + tax);
        System.out.println("Total Deductions: " + totalDeductions);
        System.out.println("Net Pay: " + netPay);
    }
}
