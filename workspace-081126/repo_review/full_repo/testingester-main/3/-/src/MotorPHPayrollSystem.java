import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * MotorPH Payroll System - Terminal Assessment Submission -
 * Comprog1 - 21 Mar 26
 * Created by: Katallino
 * * This program handles employee login, data retrieval from a CSV file, 
 * and payroll calculation for June to December.
 */

public class MotorPHPayrollSystem {

    // --- CONSTANTS (Coach Fix #1: Replacing Magic Numbers) ---
    // Using names instead of raw numbers like 0, 3, or 18 makes the code easier to read.
    private static final int ID_COL = 0;
    private static final int LAST_NAME_COL = 1;
    private static final int FIRST_NAME_COL = 2;
    private static final int BDAY_COL = 3;
    private static final int HOURLY_RATE_COL = 18;
    private static final int MIN_COL_COUNT = 19; 

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // --- LOGIN SYSTEM ---
        System.out.println("=======================================");
        System.out.println("        MOTORPH SYSTEM LOGIN           ");
        System.out.println("=======================================");
        System.out.print("Enter Username: ");
        String username = sc.nextLine();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        if (!password.equals("12345") || (!username.equals("employee") && !username.equals("payroll_staff"))) {
            System.out.println("Incorrect username and/or password.");
            return; // Terminate program
        }

        // Load Data from CSV
        String filePath = "employee_data.csv";
        String[][] employees = readFromFile(filePath);

        if (username.equals("employee")) {
            handleEmployeeMenu(employees, sc);
        } else {
            handleStaffMenu(employees, sc);
        }
    }

    // --- MENU FOR USERNAME: employee ---
    public static void handleEmployeeMenu(String[][] employees, Scanner sc) {
        System.out.println("\n1. Enter your employee number");
        System.out.println("2. Exit the program");
        System.out.print("Choice: ");
        int choice = Integer.parseInt(sc.nextLine());

        if (choice == 1) {
            System.out.print("Enter Employee #: ");
            String id = sc.nextLine();
            int index = findEmployeeIndex(employees, id);

            if (index == -1) {
                System.out.println("Employee number does not exist.");
            } else {
                System.out.println("\nEmployee Number: " + employees[index][0]);
                System.out.println("Employee Name  : " + employees[index][1]);
                System.out.println("Birthday       : " + employees[index][2]);
            }
        }
    }

    // --- MENU FOR USERNAME: payroll_staff ---
    public static void handleStaffMenu(String[][] employees, Scanner sc) {
        System.out.println("\n1. Process Payroll");
        System.out.println("2. Exit the program");
        System.out.print("Choice: ");
        int choice = Integer.parseInt(sc.nextLine());

        if (choice == 1) {
            System.out.println("\n--- SUB-OPTIONS ---");
            System.out.println("1. One employee");
            System.out.println("2. All employees");
            System.out.println("3. Exit the program");
            System.out.print("Choice: ");
            int subChoice = Integer.parseInt(sc.nextLine());

            if (subChoice == 1) {
                System.out.print("Enter employee number: ");
                String id = sc.nextLine();
                int index = findEmployeeIndex(employees, id);
                if (index == -1) {
                    System.out.println("Employee number does not exist.");
                } else {
                    generatePayroll(employees, index);
                }
            } else if (subChoice == 2) {
                // --- Coach Fix #2: Added a separator before the loop starts ---
                System.out.println("\n*** STARTING BATCH PROCESSING FOR ALL EMPLOYEES ***");
                
                for (int i = 1; i < employees.length; i++) {
                    if (employees[i][0] != null) {
                        // Added a clear visual break between each employee's data
                        System.out.println("\n============================================================");
                        System.out.println("PROCESSING RECORD: " + i);
                        System.out.println("============================================================");
                        
                        generatePayroll(employees, i);
                    }
                }
                System.out.println("\n*** BATCH PROCESSING COMPLETE ***");
            }
        }
    }

    // --- PAYROLL GENERATION (June to December) ---
    public static void generatePayroll(String[][] employees, int idx) {
        String[] months = {"June", "July", "August", "September", "October", "November", "December"};
        
        System.out.println("\n------------------------------------------------------------");
        System.out.println("Employee #: " + employees[idx][0]);
        System.out.println("Employee Name: " + employees[idx][1]);
        System.out.println("Birthday: " + employees[idx][2]);

        double hourlyRate = parseCurrency(employees[idx][4]);

        for (String month : months) {
            // 1st Cutoff (1-15) - No Deductions
            double hours1 = calculateAttendanceHours(); 
            double gross1 = hours1 * hourlyRate;
            System.out.println("\nCutoff Date: " + month + " 1 to " + month + " 15");
            System.out.println("Total Hours Worked: " + hours1);
            System.out.println("Gross Salary: " + gross1);
            System.out.println("Net Salary: " + gross1);

            // 2nd Cutoff (16-30) - Includes Deductions
            double hours2 = calculateAttendanceHours();
            double gross2 = hours2 * hourlyRate;
            
            double monthlyGross = gross1 + gross2;
            double sss = monthlyGross * 0.045;
            double phil = monthlyGross * 0.03;
            double pagibig = 100.00;
            double tax = (monthlyGross > 10000) ? (monthlyGross - 10000) * 0.15 : 0;
            double totalDeduct = sss + phil + pagibig + tax;

            System.out.println("\nCutoff Date: " + month + " 16 to " + month + " 30 (Second payout includes all deductions)");
            System.out.println("Total Hours Worked: " + hours2);
            System.out.println("Gross Salary: " + gross2);
            System.out.println("SSS: " + sss);
            System.out.println("PhilHealth: " + phil);
            System.out.println("Pag-IBIG: " + pagibig);
            System.out.println("Tax: " + tax);
            System.out.println("Total Deductions: " + totalDeduct);
            System.out.println("Net Salary: " + (gross2 - totalDeduct));
        }
    }

    public static double calculateAttendanceHours() {
        return 80.0; // Placeholder for a 15-day period
    }

    // --- CSV HELPER METHODS ---
    public static String[][] readFromFile(String filePath) {
        String[][] employees = new String[100][6];
        int index = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null && index < 100) {
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                
                // --- Coach Fix #1: Using the constants we defined at the top ---
                if (data.length >= MIN_COL_COUNT) {
                    employees[index][0] = data[ID_COL]; 
                    employees[index][1] = data[FIRST_NAME_COL].replace("\"", "") + " " + data[LAST_NAME_COL].replace("\"", ""); 
                    employees[index][2] = data[BDAY_COL]; 
                    employees[index][4] = data[HOURLY_RATE_COL]; 
                }
                index++;
            }
        } catch (Exception e) { 
            System.out.println("Error reading file.");
        }
        return employees;
    }

    public static int findEmployeeIndex(String[][] employees, String id) {
        for (int i = 0; i < employees.length; i++) {
            if (employees[i][0] != null && employees[i][0].equals(id)) return i;
        }
        return -1;
    }

    public static double parseCurrency(String value) {
        if (value == null) return 0;
        try { return Double.parseDouble(value.replaceAll("[,\"]", "")); } catch (Exception e) { return 0; }
    }
}