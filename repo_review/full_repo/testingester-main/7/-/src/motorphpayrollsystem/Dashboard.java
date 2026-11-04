/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package motorphpayrollsystem;

import java.util.Scanner;
import javax.swing.JFrame; // Dynamic UI container import requirement

/**
 * author: 
 */
public class Dashboard {
    
    static Scanner scan = new Scanner(System.in);
    static int choice = -1;
    
    public static void showMainMenu() {
        System.out.println("================================================");
        System.out.println("MOTORPH PAYROLL SYSTEM");
        System.out.println("================================================");
        System.out.println("[1] PROFILE");
        System.out.println("[2] PAYSLIP");
        System.out.println("[3] ATTENDANCE");
        System.out.println("[4] ADMIN MODULE");
        System.out.println("[0] Lgout");
        System.out.println("=================================================");
        System.out.println("ENTER SELECTION:  ");
    }
        
    public static void showAdminMenu() {
        System.out.println("================================================");
        System.out.println("ADMIN MODULE");
        System.out.println("================================================");
        System.out.println("[1] ADD EMPLOYEE");
        System.out.println("[2] DELETE EMPLOYEE");
        System.out.println("[3] UPDATE PROFILE");
        System.out.println("[4] MAIN MENU");
        System.out.println("=================================================");
        System.out.println("ENTER SELECTION:  ");
    }

    public static int menuChoice() {
        try {
            return Integer.parseInt(scan.nextLine());
        } catch (Exception e) {
            return -1;
        }
    }
        
    public static void screenPause(){
        System.out.print("\nPress ENTER to return to the menu...");
        scan.nextLine();
    }
    
    public static void mainMenu(String empNo, String[] emp){
        while (choice != 0) {
            showMainMenu();
            choice = menuChoice();
               
            switch (choice){
                case 1: 
                    // Temporarily disabled: Method missing from EmployeeData.java
                    // EmployeeData.showEmployeeDetails(emp);
                    System.out.println("\nProfile View is temporarily unavailable.");
                    screenPause();
                    break;
                    
                case 2: 
                    // This directly triggers your Attendance and Payroll calculations!
                    //Payroll.computePayroll(emp);
                    screenPause();
                    break;
                       
                case 3: 
                    // WIRED: This launches your beautiful custom visual module interface panel!
                    System.out.println("\nLaunching your visual Attendance Module screen interface...");
                    
                    JFrame attendanceWindow = new JFrame("MotorPH Attendance Management System");
                    attendanceWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Close window without closing entire app
                    attendanceWindow.setSize(740, 520); // Window dimension settings
                    attendanceWindow.setLocationRelativeTo(null); // Center-screen coordinate placement
                    
                    // Call the procedural panel generator and add it to the window frame layer
                    attendanceWindow.add(AttendanceModule.buildAttendancePanel(attendanceWindow, emp));
                    attendanceWindow.setVisible(true); // Bring window to screen visibility
                    
                    screenPause();
                    break;
                    
                case 4: 
                    adminModule(empNo, emp);
                    screenPause();
                    break;
                    
                case 0: 
                    System.out.println("Logging Out...");
                    System.exit(0);
                    break;
                    
                default: 
                    System.out.println("Invalid option!");
            }
        }             
    }
     
    public static void adminModule(String empNo, String[] emp){
        while (choice != 0) {
            showAdminMenu();
            choice = menuChoice();

            switch (choice){
                case 1: 
                    // Temporarily disabled: Requires 21 arguments in EmployeeData.java
                    // EmployeeData.addEmployee();
                    System.out.println("\nAdd Employee action is under construction.");
                    screenPause();
                    break;
                    
                case 2: 
                    // Temporarily disabled: Requires parameter arguments in EmployeeData.java
                    // EmployeeData.deleteEmployee(); 
                    System.out.println("\nDelete Employee action is under construction.");
                    screenPause();
                    break;
                    
                case 3: 
                    // Temporarily disabled: Requires parameter arguments in EmployeeData.java
                    // EmployeeData.updateProfile();
                    System.out.println("\nUpdate Profile action is under construction.");
                    screenPause();
                    break;
                    
                case 4: 
                    mainMenu(empNo, emp);
                    screenPause();
                    break;
                    
                case 0: 
                default: 
                    System.out.println("Invalid option!");
            } 
        }       
    }
}