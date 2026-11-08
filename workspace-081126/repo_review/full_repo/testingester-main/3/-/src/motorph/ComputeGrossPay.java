/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package motorph;

/**
 *
 * @author Katallino
 */
public class ComputeGrossPay {

    public static double ComputeGrossPay(double hoursWorked, double hourlyRate) {
        return hoursWorked * hourlyRate;
    }

    public static void main(String[] args) {

        String employeeName = "Juan Dela Cruz";
        int employeeId = 1001;

        double hoursWorked = 40;
        double hourlyRate = 150;

        double grossPay = ComputeGrossPay(hoursWorked, hourlyRate);

        System.out.println("Employee: " + employeeName);
        System.out.println("Employee ID: " + employeeId);
        System.out.println("Hours Worked: " + hoursWorked);
        System.out.println("Hourly Rate: " + hourlyRate);
        System.out.println("Gross Pay: " + grossPay);
    }
    }
    

