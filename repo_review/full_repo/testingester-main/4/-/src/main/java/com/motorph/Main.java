package com.motorph;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.motorph.controller.EmployeeController;
import com.motorph.controller.PayrollController;
import com.motorph.controller.ReportController;
import com.motorph.model.AttendanceRecord;
import com.motorph.model.Employee;
import com.motorph.repository.DataRepository;
import com.motorph.service.EmployeeService;
import com.motorph.service.PayrollProcessor;
import com.motorph.service.PayrollService;
import com.motorph.service.ReportService;
import com.motorph.util.AppConstants;
import com.motorph.util.AppUtils;
import com.motorph.view.Login;
import com.motorph.view.MainFrame;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(() -> {
                try {
                    showLoginScreen();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to initialize application", e);
                    System.exit(1);
                }
            });
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
                | IllegalAccessException e) {
            logger.log(Level.WARNING, "Failed to set system look and feel", e);
            SwingUtilities.invokeLater(() -> {
                try {
                    showLoginScreen();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Failed to initialize application", ex);
                    System.exit(1);
                }
            });
        }
    }

    private static void initializeDefaultSession() {
        try {
            com.motorph.model.User defaultUser = new com.motorph.model.User("admin", "password", 1, "ADMIN", true);
            AppUtils.setCurrentUser(defaultUser);
            logger.log(Level.INFO, "Default session initialized for user: {0}", defaultUser.getUsername());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to initialize default session", e);
        }
    }

    public static void showLoginScreen() {
        showLoginScreen(null);
    }

    public static void showLoginScreen(Runnable onLoginSuccess) {
        Login loginFrame = new Login(() -> {
            try {
                if (onLoginSuccess != null) {
                    onLoginSuccess.run();
                } else {
                    initializeApplication();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to initialize main application after login", e);
                System.exit(1);
            }
        });
        loginFrame.showLogin();
    }

    public static void initializeApplication() throws IOException {
        String employeesFilePath = AppConstants.getEmployeeFilePath();
        String attendanceFilePath = AppConstants.getAttendanceFilePath();

        DataRepository dataRepository = new DataRepository(employeesFilePath, attendanceFilePath);
        List<Employee> employees = dataRepository.getAllEmployees();
        List<AttendanceRecord> attendanceRecords = dataRepository.getAllAttendanceRecords();

        PayrollProcessor payrollCalculator = new PayrollProcessor();

        // Updated EmployeeService constructor (matches new code)
        EmployeeService employeeService = new EmployeeService(employees, attendanceRecords, employeesFilePath);

        PayrollService payrollService = new PayrollService(employees, attendanceRecords, payrollCalculator);
        ReportService reportService = new ReportService(employeeService, payrollService);

        EmployeeController employeeController = new EmployeeController(employeeService);
        PayrollController payrollController = new PayrollController(payrollService);
        ReportController reportController = new ReportController(reportService);

        MainFrame mainFrame = new MainFrame(employeeController, payrollController, reportController);
        mainFrame.setVisible(true);

        logger.info("MotorPH Payroll System started successfully");
    }
}