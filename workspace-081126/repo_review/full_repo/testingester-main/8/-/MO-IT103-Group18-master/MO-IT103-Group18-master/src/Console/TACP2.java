package Console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;


public class TACP2 {

    private static final String EMP_DETAILS_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vRSib3-eoyhZfsNIGL0kj2cHNhcRw6efjoE63jtm4mizONxWaGfNyDFCA_e9BboKEKZOY6qhQxbUS6v/pub?output=csv";
    private static final String EMP_PROFILE_GID = "1510394485";
    private static final String EMP_ATTENDANCE_GID = "1129354034";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final double HOURLY_RATE = 500; // Define hourly rate

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            login(scanner);
        } finally {
            scanner.close(); // Close the scanner in a finally block
        }
    }

    private static void login(Scanner scanner) {
        System.out.print("Username (Employee Number): ");
        String empNumber = scanner.nextLine().trim();

        System.out.print("Password (Last Name): ");
        String lastName = scanner.nextLine().trim();

        if (checkCredentials(empNumber, lastName)) {
            System.out.println("\nLogin successful! Welcome, Employee #" + empNumber + ".");
            mainMenu(scanner, empNumber);
        } else {
            System.out.println("Invalid Employee Number or Last Name.");
        }
    }

    private static void mainMenu(Scanner scanner, String empNumber) {
        while (true) {
            System.out.println("\nPlease choose an option:\n");
            System.out.println("1. View Profile");
            System.out.println("2. View Attendance");
            System.out.println("3. View Payroll");
            System.out.println("4. Logout");
            System.out.println("----------------------------");
            System.out.print("\nEnter your choice: ");

            int choice = -1;
            try {
                choice = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number between 1 and 4.");
                scanner.next(); // Consume the invalid input
                continue;
            }
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    viewProfile(empNumber);
                    break;
                case 2:
                    viewAttendance(scanner, empNumber);
                    break;
                case 3:
                    viewPayroll(scanner, empNumber);
                    break;
                case 4:
                    System.out.println("LOGGING OUT...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    public static boolean checkCredentials(String empNumber, String lastName) {
        try {
            String urlWithGid = EMP_DETAILS_URL + "&gid=" + EMP_PROFILE_GID;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithGid).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch data. Response Code: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] credentials = line.split(",");
                if (credentials.length >= 4 && credentials[0].trim().equals(empNumber) && credentials[1].trim().equalsIgnoreCase(lastName)) {
                    return true;
                }
            }
            br.close();
            conn.disconnect();
        } catch (IOException e) {
            System.out.println("Error accessing Google Sheets: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static void viewProfile(String empNumber) {
        try {
            String urlWithGid = EMP_DETAILS_URL + "&gid=" + EMP_PROFILE_GID;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithGid).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch data. Response Code: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].trim().equals(empNumber)) {
                    System.out.println("\nPROFILE: ");
                    System.out.println("Full Name: " + data[2].trim() + " " + data[1].trim());
                    System.out.println("Employee Number: " + data[0].trim());
                    System.out.println("Birthday: " + data[3].trim());
                    System.out.println("----------------------------");
                    br.close();
                    conn.disconnect();
                    return;
                }
            }
            System.out.println("Profile not found for employee number " + empNumber);
            br.close();
            conn.disconnect();

        } catch (IOException e) {
            System.out.println("Error accessing Google Sheets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void viewAttendance(Scanner scanner, String empNumber) {
        while (true) {
            System.out.println("\nATTENDANCE MENU:");
            System.out.println("\n1. Daily Attendance Records");
            System.out.println("2. Calculate Hours Worked Per Week");
            System.out.println("3. Back to Main Menu");
            System.out.println("||----------------------------||");
            System.out.print("\nEnter your choice: ");

            int choice = -1;
            try {
                choice = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Invalid choice. Please enter a number.");
                scanner.next();
                continue;
            }
            scanner.nextLine();

            switch (choice) {
                case 1:
                    listAttendance(empNumber);
                    break;
                case 2:
                    calculateWeeklyHours(empNumber);
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    public static void listAttendance(String empNumber) {
        try {
            String urlWithGid = EMP_DETAILS_URL + "&gid=" + EMP_ATTENDANCE_GID;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithGid).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch data. Response Code: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            br.readLine();

            boolean attendanceFound = false;
            System.out.println("ATTENDANCE RECORDS:");
            System.out.println("===================================================");

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 6 && data[0].trim().equals(empNumber)) {
                    double hoursWorked = calculateHoursWorked(data[4].trim(), data[5].trim());
                    System.out.println("Date: " + data[3].trim() + " | Employee #: " + data[0].trim() + " | Hours Worked: " + hoursWorked);
                    attendanceFound = true;
                }
            }

            if (!attendanceFound) {
                System.out.println("No attendance records found for Employee #" + empNumber);
            }

            System.out.println("===================================================");
            br.close();
            conn.disconnect();

        } catch (IOException e) {
            System.out.println("Error accessing Google Sheets: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error calculating hours: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static double calculateHoursWorked(String logIn, String logOut) throws Exception {
        if (logIn == null || logIn.isEmpty() || logOut == null || logOut.isEmpty()) {
            throw new IllegalArgumentException("Login or logout time is missing or invalid.");
        }
        try {
            LocalTime inTime = LocalTime.parse(logIn, TIME_FORMAT);
            LocalTime outTime = LocalTime.parse(logOut, TIME_FORMAT);

            if (outTime.isBefore(inTime)) {
                throw new IllegalArgumentException("Logout time cannot be before login time.");
            }
            Duration duration = Duration.between(inTime, outTime);
            return duration.toMinutes() / 60.0 - 1;
        } catch (DateTimeParseException e) {
            throw new Exception("Error parsing time: " + e.getMessage() + ".  Login Time: " + logIn + " Logout Time: " + logOut);
        }
    }

    public static void calculateWeeklyHours(String empNumber) {
        try {
            String urlWithGid = EMP_DETAILS_URL + "&gid=" + EMP_ATTENDANCE_GID;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithGid).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch data. Response Code: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            br.readLine();

            Map<Integer, Double> weeklyHours = new HashMap<>();

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 6 && data[0].trim().equals(empNumber)) {
                    try {
                        LocalDate date = LocalDate.parse(data[3].trim(), DATE_FORMAT);
                        int weekOfYear = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
                        double hoursWorked = calculateHoursWorked(data[4].trim(), data[5].trim());
                        weeklyHours.put(weekOfYear, weeklyHours.getOrDefault(weekOfYear, 0.0) + hoursWorked);
                    } catch (DateTimeParseException e) {
                        System.out.println("Error parsing date: " + data[3].trim() + "  Error: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Error calculating hours for weekly: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            br.close();
            conn.disconnect();

            if (weeklyHours.isEmpty()) {
                System.out.println("No weekly hours data found.");
                return;
            }

            System.out.println("WEEK # | TOTAL HOURS WORKED");
            System.out.println("------------------------------------");
            for (Map.Entry<Integer, Double> entry : weeklyHours.entrySet()) {
                System.out.printf("Week %d    | %.2f hours\n", entry.getKey(), entry.getValue());
            }

        } catch (IOException e) {
            System.out.println("Error accessing Google Sheets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void viewPayroll(Scanner scanner, String empNumber) {
        System.out.print("Enter total hours worked: ");
        double hoursWorked = 0;
        try{
             hoursWorked = scanner.nextDouble();
        } catch(InputMismatchException e){
            System.out.println("Invalid hours worked input");
            return;
        }

        scanner.nextLine();

        double grossSalary = computeGrossSalary(hoursWorked);

        System.out.println("Debug: Hours Worked Entered = " + hoursWorked);
        System.out.println("Debug: Gross Salary Computed = " + grossSalary);

        while (true) {
            System.out.println("\nPAYROLL MENU:");
            System.out.println("1. View Salary");
            System.out.println("2. View Deductions");
            System.out.println("3. View Payslip");
            System.out.println("4. Back to Main Menu");
            System.out.println("----------------------------");
            System.out.print("\nEnter your choice: ");
            int choice = -1;
            try {
                choice = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Invalid choice. Please enter a number.");
                scanner.next();
                continue;
            }
            scanner.nextLine();

            switch (choice) {
                case 1:
                    calculateSalary(empNumber);
                    break;
                case 2:
                    calculateDeductions(empNumber, grossSalary);
                    break;
                case 3:
                    calculatePayslip(empNumber, grossSalary);
                    break;    
                case 4:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }



    public static void calculateSalary(String empNumber) {
        try {
            String urlWithGid = EMP_DETAILS_URL + "&gid=" + EMP_PROFILE_GID;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithGid).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch data. Response Code: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            br.readLine();

            String salaryStr = "5,000";
            double salary = 5000.0;
            boolean salaryFound = false;

            while ((salaryStr = br.readLine()) != null) {
                String[] data = salaryStr.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (data.length >= 18 && data[0].trim().equals(empNumber)) {
                    try {
                        salaryStr = data[13].trim().replaceAll("[\",]", "").trim();
                          
                         //other compensation
                        String riceSubsidy = data[14].trim().replaceAll("[\",]", "");
                        String phoneAllowance = data[15].trim().replaceAll("[\",]", "");
                        String clothingAllowance = data[16].trim().replaceAll("[\",]", "");
                        String grossSemiMonthlyRate = data[17].trim().replaceAll("[\",]", "");
                        String hourlyRate = data[18].trim().replaceAll("[\",]", "");

                        // Convert to double AFTER cleaning
                        salary = Double.parseDouble(salaryStr);                    
                        System.out.println("Salary: " + salary);

                        salaryFound = true;

                        // Print Additional Data
                        System.out.println("Rice Subsidy: PHP " + riceSubsidy);
                        System.out.println("Phone Allowance: PHP " + phoneAllowance);
                        System.out.println("Clothing Allowance: PHP " + clothingAllowance);
                        System.out.println("Gross Semi-Monthly Rate: PHP " + grossSemiMonthlyRate);
                        System.out.println("Hourly Rate: PHP " + hourlyRate);
                        System.out.println("----------------------------");

                        return;

                    } catch (NumberFormatException e) {
                        System.out.println("Error: Invalid number format in salary data. " + e.getMessage());
                        e.printStackTrace();
                        br.close();
                        conn.disconnect();
                        return;
                    }
                }
            }
            if (!salaryFound) {
                System.out.println("Error: No salary data found for Employee #" + empNumber);
            }
            br.close();
            conn.disconnect();
        } catch (IOException e) {
            System.out.println("Error accessing Google Sheets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void calculatePayslip(String empNumber, double grossSalary) {
        try {
            String urlWithGid = EMP_DETAILS_URL + "&gid=" + EMP_PROFILE_GID;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithGid).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            br.readLine();
         //other compensation
            double riceSubsidy = 0.0;
            double phoneAllowance = 0.0;        
            double clothingAllowance = 0.0;        

            String line;
            while ((line = br.readLine()) != null) {
            String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            
            if (data.length >= 18 && data[0].trim().equals(empNumber)) { 
                // Extract Allowances
                riceSubsidy = Double.parseDouble(data[14].trim().replaceAll("[\",]", ""));
                phoneAllowance = Double.parseDouble(data[15].trim().replaceAll("[\",]", ""));
                clothingAllowance = Double.parseDouble(data[16].trim().replaceAll("[\",]", ""));
                break;}
            }                 
        double sss = computeSSS(grossSalary);
        double philHealth = computePhilHealth(grossSalary);
        double pagIbig = computePagIbig(grossSalary);
        double withholdingTax = computeWithholdingTax(grossSalary);

        double totalDeductions = sss + philHealth + pagIbig + withholdingTax;
        double totalAllowance = riceSubsidy + phoneAllowance + clothingAllowance;
        double netSalary = grossSalary - totalDeductions;

        System.out.println("\n--- Payslip details for Employee #" + empNumber + " ---");
        System.out.printf("Gross Salary: PHP %.2f%n", grossSalary);
        System.out.printf("Net Salary: PHP %.2f%n", netSalary + totalAllowance);
        System.out.println("-------------------------------------------");
        System.out.println("Breakdown of deductions and other Compensation");
        System.out.println(" ");
        System.out.printf("TOTAL ALLOWANCE: PHP %.2f%n", totalAllowance);
        System.out.println("Rice Subsidy: PHP " + riceSubsidy);
        System.out.println("Phone Allowance: PHP " + phoneAllowance);
        System.out.println("Clothing Allowance: PHP " + clothingAllowance);
        System.out.println(" ");
        System.out.printf("TOTAL DEDUCTIONS: PHP %.2f%n", totalDeductions);
        System.out.printf("SSS: PHP %.2f%n", sss);
        System.out.printf("PhilHealth: PHP %.2f%n", philHealth);
        System.out.printf("Pag-IBIG: PHP %.2f%n", pagIbig);
        System.out.printf("Withholding Tax: PHP %.2f%n", withholdingTax);
        

        
    } catch (Exception e) {
        System.out.println("Error calculating deductions: " + e.getMessage());
    }
}
    public static double computeGrossSalary(double hoursWorked) {
        return hoursWorked * HOURLY_RATE + 1000;
    }

    private static void calculateDeductions(String empNumber, double grossSalary) {
        try {
            String urlWithGid = EMP_DETAILS_URL + "&gid=" + EMP_PROFILE_GID;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithGid).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            br.readLine();
         //other compensation
            double riceSubsidy = 0.0;
            double phoneAllowance = 0.0;        
            double clothingAllowance = 0.0;        

            String line;
            while ((line = br.readLine()) != null) {
            String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            
            if (data.length >= 18 && data[0].trim().equals(empNumber)) { 
                // Extract Allowances
                riceSubsidy = Double.parseDouble(data[14].trim().replaceAll("[\",]", ""));
                phoneAllowance = Double.parseDouble(data[15].trim().replaceAll("[\",]", ""));
                clothingAllowance = Double.parseDouble(data[16].trim().replaceAll("[\",]", ""));
                break;}
            }                 
        double sss = computeSSS(grossSalary);
        double philHealth = computePhilHealth(grossSalary);
        double pagIbig = computePagIbig(grossSalary);
        double withholdingTax = computeWithholdingTax(grossSalary);

        double totalDeductions = sss + philHealth + pagIbig + withholdingTax;
        double totalAllowance = riceSubsidy + phoneAllowance + clothingAllowance;
        double netSalary = grossSalary - totalDeductions;

        System.out.println("\n--- Salary details with Deductions for Employee #" + empNumber + " ---");
        System.out.printf("Gross Salary: PHP %.2f%n", grossSalary);
        System.out.printf("SSS: PHP %.2f%n", sss);
        System.out.printf("PhilHealth: PHP %.2f%n", philHealth);
        System.out.printf("Pag-IBIG: PHP %.2f%n", pagIbig);
        System.out.printf("Withholding Tax: PHP %.2f%n", withholdingTax);
        System.out.printf("Total Deductions: PHP %.2f%n", totalDeductions);
        System.out.printf("Net Salary: PHP %.2f%n", netSalary);
        
    } catch (Exception e) {
        System.out.println("Error calculating deductions: " + e.getMessage());
    }
}

    public static double computeSSS(double grossSalary) {
        if (grossSalary <= 3250) {
            return 135;
        } else if (grossSalary <= 3750) {
            return 157.50;
        } else if (grossSalary <= 4250) {
            return 180;
        } else if (grossSalary <= 4750) {
            return 202.50;
        } else if (grossSalary <= 5250) {
            return 225;
        } else if (grossSalary <= 5750) {
            return 247.50;
        } else if (grossSalary <= 6250) {
            return 270;
        } else if (grossSalary <= 6750) {
            return 292.50;
        } else if (grossSalary <= 7250) {
            return 315;
        } else if (grossSalary <= 7750) {
            return 337.50;
        } else if (grossSalary <= 8250) {
            return 360;
        } else if (grossSalary <= 8750) {
            return 382.50;
        } else if (grossSalary <= 9250) {
            return 405;
        } else if (grossSalary <= 9750) {
            return 427.50;
        } else if (grossSalary <= 10250) {
            return 450;
        } else if (grossSalary <= 10750) {
            return 472.50;
        } else if (grossSalary <= 11250) {
            return 495;
        } else if (grossSalary <= 11750) {
            return 517.50;
        } else if (grossSalary <= 12250) {
            return 540;
        } else if (grossSalary <= 12750) {
            return 562.50;
        } else if (grossSalary <= 13250) {
            return 585;
        } else if (grossSalary <= 13750) {
            return 607.50;
        } else if (grossSalary <= 14250) {
            return 630;
        } else if (grossSalary <= 14750) {
            return 652.50;
        } else if (grossSalary <= 15250) {
            return 675;
        } else if (grossSalary <= 15750) {
            return 697.50;
        } else if (grossSalary <= 16250) {
            return 720;
        } else if (grossSalary <= 16750) {
            return 742.50;
        } else if (grossSalary <= 17250) {
            return 765;
        } else if (grossSalary <= 17750) {
            return 787.50;
        } else if (grossSalary <= 18250) {
            return 810;
        } else if (grossSalary <= 18750) {
            return 832.50;
        } else if (grossSalary <= 19250) {
            return 855;
        } else if (grossSalary <= 19750) {
            return 877.50;
        } else if (grossSalary <= 20250) {
            return 900;
        } else if (grossSalary <= 20750) {
            return 922.50;
        } else if (grossSalary <= 21250) {
            return 945;
        } else if (grossSalary <= 21750) {
            return 967.50;
        } else if (grossSalary <= 22250) {
            return 990;
        } else if (grossSalary <= 22750) {
            return 1012.50;
        } else if (grossSalary <= 23250) {
            return 1035;
        } else if (grossSalary <= 23750) {
            return 1057.50;
        } else if (grossSalary <= 24250) {
            return 1080;
        } else if (grossSalary <= 24750) {
            return 1102.50;
        } else if (grossSalary <= 25250) {
            return 1125;
        } else if (grossSalary <= 25750) {
            return 1147.50;
        } else if (grossSalary <= 26250) {
            return 1170;
        } else if (grossSalary <= 26750) {
            return 1192.50;
        } else if (grossSalary <= 27250) {
            return 1215;
        } else if (grossSalary <= 27750) {
            return 1237.50;
        } else if (grossSalary <= 28250) {
            return 1260;
        } else if (grossSalary <= 28750) {
            return 1282.50;
        } else if (grossSalary <= 29250) {
            return 1305;
        } else if (grossSalary <= 29750) {
            return 1327.50;
        } else if (grossSalary <= 30250) {
            return 1350;
        } else if (grossSalary <= 30750) {
            return 1372.50;
        } else if (grossSalary <= 31250) {
            return 1395;
        } else if (grossSalary <= 31750) {
            return 1417.50;
        } else if (grossSalary <= 32250) {
            return 1440;
        } else if (grossSalary <= 32750) {
            return 1462.50;
        } else if (grossSalary <= 33250) {
            return 1485;
        } else if (grossSalary <= 33750) {
            return 1507.50;
        } else if (grossSalary <= 34250) {
            return 1530;
        } else if (grossSalary <= 34750) {
            return 1552.50;
        } else if (grossSalary <= 35250) {
            return 1575;
        } else if (grossSalary <= 35750) {
            return 1597.50;
        } else if (grossSalary <= 36250) {
            return 1620;
        } else if (grossSalary <= 36750) {
            return 1642.50;
        } else if (grossSalary <= 37250) {
            return 1665;
        } else if (grossSalary <= 37750) {
            return 1687.50;
        } else if (grossSalary <= 38250) {
            return 1710;
} else if (grossSalary <= 38750) {
            return 1732.50;
        } else if (grossSalary <= 39250) {
            return 1755;
        } else if (grossSalary <= 39750) {
            return 1777.50;
        } else if (grossSalary <= 40250) {
            return 1800;
        } else if (grossSalary <= 40750) {
            return 1822.50;
        } else if (grossSalary <= 41250) {
            return 1845;
        } else if (grossSalary <= 41750) {
            return 1867.50;
        } else if (grossSalary <= 42250) {
            return 1890;
        } else if (grossSalary <= 42750) {
            return 1912.50;
        } else if (grossSalary <= 43250) {
            return 1935;
        } else if (grossSalary <= 43750) {
            return 1957.50;
        } else if (grossSalary <= 44250) {
            return 1980;
        } else if (grossSalary <= 44750) {
            return 2002.50;
        } else if (grossSalary <= 45250) {
            return 2025;
        } else if (grossSalary <= 45750) {
            return 2047.50;
        } else if (grossSalary <= 46250) {
            return 2070;
        } else if (grossSalary <= 46750) {
            return 2092.50;
        } else if (grossSalary <= 47250) {
            return 2115;
        } else if (grossSalary <= 47750) {
            return 2137.50;
        } else if (grossSalary <= 48250) {
            return 2160;
        } else if (grossSalary <= 48750) {
            return 2182.50;
        } else if (grossSalary <= 49250) {
            return 2205;
        } else if (grossSalary <= 49750) {
            return 2227.50;
        } else if (grossSalary <= 50250) {
            return 2250;
        } else if (grossSalary <= 50750) {
            return 2272.50;
        } else if (grossSalary <= 51250) {
            return 2295;
        } else if (grossSalary <= 51750) {
            return 2317.50;
        } else if (grossSalary <= 52250) {
            return 2340;
        } else if (grossSalary <= 52750) {
            return 2362.50;
        } else if (grossSalary <= 53250) {
            return 2385;
        } else if (grossSalary <= 53750) {
            return 2407.50;
        } else if (grossSalary <= 54250) {
            return 2430;
        } else if (grossSalary <= 54750) {
            return 2452.50;
        } else if (grossSalary <= 55250) {
            return 2475;
        } else if (grossSalary <= 55750) {
            return 2497.50;
        } else if (grossSalary <= 56250) {
            return 2520;
        } else if (grossSalary <= 56750) {
            return 2542.50;
        } else if (grossSalary <= 57250) {
            return 2565;
        } else if (grossSalary <= 57750) {
            return 2587.50;
        } else if (grossSalary <= 58250) {
            return 2610;
        } else if (grossSalary <= 58750) {
            return 2632.50;
        } else if (grossSalary <= 59250) {
            return 2655;
        } else if (grossSalary <= 59750) {
            return 2677.50;
        } else if (grossSalary <= 60250) {
            return 2700;
        } else if (grossSalary <= 60750) {
            return 2722.50;
        } else if (grossSalary <= 61250) {
            return 2745;
        } else if (grossSalary <= 61750) {
            return 2767.50;
        } else if (grossSalary <= 62250) {
            return 2790;
        } else if (grossSalary <= 62750) {
            return 2812.50;
        } else if (grossSalary <= 63250) {
            return 2835;
        } else if (grossSalary <= 63750) {
            return 2857.50;
        } else if (grossSalary <= 64250) {
            return 2880;
        } else if (grossSalary <= 64750) {
            return 2902.50;
        } else if (grossSalary <= 65250) {
            return 2925;
        } else if (grossSalary <= 65750) {
            return 2947.50;
        } else if (grossSalary <= 66250) {
            return 2970;
        } else if (grossSalary <= 66750) {
            return 2992.50;
        } else if (grossSalary <= 67250) {
            return 3015;
        } else if (grossSalary <= 67750) {
            return 3037.50;
        } else if (grossSalary <= 68250) {
            return 3060;
        } else if (grossSalary <= 68750) {
            return 3082.50;
        } else if (grossSalary <= 69250) {
            return 3105;
        } else if (grossSalary <= 69750) {
            return 3127.50;
        } else if (grossSalary <= 70250) {
            return 3150;
        } else if (grossSalary <= 70750) {
            return 3172.50;
        } else if (grossSalary <= 71250) {
            return 3195;
        } else if (grossSalary <= 71750) {
            return 3217.50;
        } else if (grossSalary <= 72250) {
            return 3240;
        } else if (grossSalary <= 72750) {
            return 3262.50;
        } else if (grossSalary <= 73250) {
            return 3285;
        } else if (grossSalary <= 73750) {
            return 3307.50;
        } else if (grossSalary <= 74250) {
            return 3330;
        } else if (grossSalary <= 74750) {
            return 3352.50;
        } else if (grossSalary <= 75250) {
            return 3375;
        } else if (grossSalary <= 75750) {
            return 3397.50;
        } else if (grossSalary <= 76250) {
            return 3420;
        } else if (grossSalary <= 76750) {
            return 3442.50;
        } else if (grossSalary <= 77250) {
            return 3465;
        } else if (grossSalary <= 77750) {
            return 3487.50;
        } else if (grossSalary <= 78250) {
            return 3510;
        } else if (grossSalary <= 78750) {
            return 3532.50;
        } else if (grossSalary <= 79250) {
            return 3555;
        } else if (grossSalary <= 79750) {
            return 3577.50;
        } else if (grossSalary <= 80250) {
            return 3600;
        } else if (grossSalary <= 80750) {
            return 3622.50;
        } else if (grossSalary <= 81250) {
            return 3645;
        } else if (grossSalary <= 81750) {
            return 3667.50;
        } else if (grossSalary <= 82250) {
            return 3690;
        } else if (grossSalary <= 82750) {
            return 3712.50;
        } else if (grossSalary <= 83250) {
            return 3735;
        } else if (grossSalary <= 83750) {
            return 3757.50;
        } else if (grossSalary <= 84250) {
            return 3780;
        } else if (grossSalary <= 84750) {
            return 3802.50;
        } else if (grossSalary <= 85250) {
            return 3825;
        } else if (grossSalary <= 85750) {
            return 3847.50;
        } else if (grossSalary <= 86250) {
            return 3870;
        } else if (grossSalary <= 86750) {
            return 3892.50;
        } else if (grossSalary <= 87250) {
            return 3915;
        } else if (grossSalary <= 87750) {
            return 3937.50;
        } else if (grossSalary <= 88250) {
            return 3960;
        } else if (grossSalary <= 88750) {
            return 3982.50;
        } else if (grossSalary <= 89250) {
            return 4005;
        } else if (grossSalary <= 89750) {
            return 4027.50;
        } else if (grossSalary <= 90250) {
            return 4050;
        } else if (grossSalary <= 90750) {
            return 4072.50;
        } else if (grossSalary <= 91250) {
            return 4095;
        } else if (grossSalary <= 91750) {
            return 4117.50;
        } else if (grossSalary <= 92250) {
            return 4140;
        } else if (grossSalary <= 92750) {
            return 4162.50;
        } else if (grossSalary <= 93250) {
            return 4185;
        } else if (grossSalary <= 93750) {
            return 4207.50;
        } else if (grossSalary <= 94250) {
            return 4230;
        } else if (grossSalary <= 94750) {
            return 4252.50;
        } else if (grossSalary <= 95250) {
            return 4275;
        } else if (grossSalary <= 95750) {
            return 4297.50;
        } else if (grossSalary <= 96250) {
            return 4320;
        } else if (grossSalary <= 96750) {
            return 4342.50;
        } else if (grossSalary <= 97250) {
            return 4365;
        } else if (grossSalary <= 97750) {
            return 4387.50;
        } else if (grossSalary <= 98250) {
            return 4410;
        } else if (grossSalary <= 98750) {
            return 4432.50;
        } else if (grossSalary <= 99250) {
            return 4455;
        } else if (grossSalary <= 99750) {
            return 4477.50;
        } else {
            return 4500;
        }
    }

    public static double computePhilHealth(double grossSalary) {
        if (grossSalary <= 10000) {
            return grossSalary * 0.03 / 2;
        } else if (grossSalary <= 59999.99) {
            return grossSalary * 0.03 / 2;
        } else if (grossSalary >= 60000) {
            return 900;
        }
        return 0;
    }

    public static double computePagIbig(double grossSalary) {
        return Math.min(grossSalary * 0.02, 100);
    }

    public static double computeWithholdingTax(double grossSalary) {
        if (grossSalary <= 20833) {
            return 0;
        } else if (grossSalary <= 33332) {
            return (grossSalary - 20833) * 0.20;
        } else if (grossSalary <= 66666) {
            return 2500 + (grossSalary - 33332) * 0.25;
        } else if (grossSalary <= 166666) {
            return 10833.33 + (grossSalary - 66666) * 0.30;
        } else if (grossSalary <= 666666) {
            return 40833.33 + (grossSalary - 166666) * 0.32;
        } else {
            return 200833.33 + (grossSalary - 666666) * 0.35;
        }
    }
}

