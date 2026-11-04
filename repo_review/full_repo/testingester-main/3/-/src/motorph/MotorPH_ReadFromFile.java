 
         
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Katallino
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class MotorPH_ReadFromFile {

    // Government deduction methods
    public static double computeSSS(double salary) {
        return salary * 0.045;
    }

    public static double computePhilHealth(double salary) {
        return salary * 0.03;
    }

    public static double computePagIbig(double salary) {
        return 100;
    }

    public static double computeTax(double salary) {
        return salary * 0.10;
    }

    public static void main(String[] args) {

        // CSV file name
        String fileName = "employee_data.csv";

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));

            String line;

            // Skip the header row of the CSV
            br.readLine();

            // Read each employee record
            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");

                // Combine first and last name
                String lastName = data[1];
                String firstName = data[2];
                String name = firstName + " " + lastName;

                // Gross semi-monthly salary column
                String salaryText = data[17];

                // Remove commas from numbers like "45,000"
                salaryText = salaryText.replace(",", "");

                salaryText = salaryText.replace(",", "").replace("\"", "").trim();
                double salary = Double.parseDouble(salaryText);

                // Compute deductions
                double sss = computeSSS(salary);
                double philhealth = computePhilHealth(salary);
                double pagibig = computePagIbig(salary);
                double tax = computeTax(salary);

                double netPay = salary - (sss + philhealth + pagibig + tax);

                // Display payroll summary
                System.out.println("=================================");
                System.out.println("Employee: " + name);
                System.out.println("Gross Salary: " + salary);
                System.out.println("SSS: " + sss);
                System.out.println("PhilHealth: " + philhealth);
                System.out.println("Pag-IBIG: " + pagibig);
                System.out.println("Income Tax: " + tax);
                System.out.println("Net Pay: " + netPay);
                System.out.println("=================================");
            }

            br.close();

        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }
    }
}
    
