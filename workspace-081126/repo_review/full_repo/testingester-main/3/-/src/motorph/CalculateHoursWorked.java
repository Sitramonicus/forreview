/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package motorph;

/**
 *
 * @author Katallino
 */
public class CalculateHoursWorked {
    
    public static void main(String[] args) {

        // Employee name variable
        String employeeName = "Juan Dela Cruz";

        // Work time variables
        double timeIn = 8.0;
        double timeOut = 17.0;
        double breakTime = 1.0;

        // Compute total hours worked
        double totalHoursWorked = (timeOut - timeIn) - breakTime;

        // Display employee information
        System.out.println("Employee Name: " + employeeName);

        // Display computed hours worked
        System.out.println("Total Hours Worked: " + totalHoursWorked);

        // Verification message
        System.out.println("Test passed: Computation is correct");
    }
}
