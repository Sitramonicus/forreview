    package com.motorph.view;

    import javax.swing.*;
    import java.awt.*;
    import com.motorph.controller.*;
    import com.motorph.util.AppConstants;
    import com.motorph.util.AppUtils;
    import com.motorph.security.RolePermission;

    public class MainFrame extends JFrame {
        private final EmployeeController employeeController;
        private final PayrollController payrollController;
        private final ReportController reportController;
        private CardLayout cardLayout;
        private JPanel cardPanel;
        private HeaderPanel headerPanel;
        private JPanel sideNavPanel;

        // Panels
        private Dashboard dashboardPanel;
        private EmployeePanel employeePanel;
        private Payroll payrollPanel;
        private Reports reportsPanel;
        private LeavePanel leavePanel;

        private RolePermission rolePermission; // RolePermission instance

        public MainFrame(EmployeeController employeeController,
                         PayrollController payrollController,
                         ReportController reportController) {
            super(AppConstants.APP_TITLE);

            this.employeeController = employeeController;
            this.payrollController = payrollController;
            this.reportController = reportController;

            // Initialize role permission
            this.rolePermission = new RolePermission(AppUtils.getCurrentUser().getRole());

            initUI();
        }

        private void initUI() {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception e) {
                System.err.println("Could not set system look and feel: " + e.getMessage());
            }

            setSize(1400, 900);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            // Header
            headerPanel = new HeaderPanel();

            // Side navigation
            sideNavPanel = createSideNavigationPanel();

            // Card layout for main content
            cardLayout = new CardLayout();
            cardPanel = new JPanel(cardLayout);
            cardPanel.setBackground(AppConstants.BACKGROUND_COLOR);

            // Initialize panels
            dashboardPanel = new Dashboard(this, employeeController);
            employeePanel = new EmployeePanel(this, employeeController);
            payrollPanel = new Payroll(this, payrollController);
            reportsPanel = new Reports(this, reportController);
            leavePanel = new LeavePanel();

            // Add panels to card layout
            cardPanel.add(dashboardPanel, "MainMenu");
            cardPanel.add(employeePanel, "EmployeeList");
            cardPanel.add(payrollPanel, "PayrollManagement");
            cardPanel.add(reportsPanel, "Reports");
            cardPanel.add(leavePanel, "LeavePanel");

            // Show the main menu only if user has access
            if (rolePermission.canAccessDashboard()) {
                cardLayout.show(cardPanel, "MainMenu");
            }

            // Main content panel
            JPanel mainContentPanel = new JPanel(new BorderLayout());
            mainContentPanel.add(headerPanel, BorderLayout.NORTH);
            mainContentPanel.add(cardPanel, BorderLayout.CENTER);

            add(sideNavPanel, BorderLayout.WEST);
            add(mainContentPanel, BorderLayout.CENTER);
        }

        private JPanel createSideNavigationPanel() {
            JPanel sidePanel = new JPanel();
            sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
            sidePanel.setBackground(AppConstants.SIDEBAR_BACKGROUND);
            sidePanel.setPreferredSize(new Dimension(280, 0));
            sidePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(105, 8, 32)));

            // Brand/logo
            JPanel brandPanel = new JPanel();
            brandPanel.setBackground(AppConstants.SIDEBAR_BACKGROUND);
            brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
            JLabel brandLabel = new JLabel("MotorPH");
            brandLabel.setForeground(Color.WHITE);
            brandLabel.setFont(AppConstants.TITLE_FONT);
            brandLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            brandPanel.add(Box.createVerticalStrut(20));
            brandPanel.add(brandLabel);

            sidePanel.add(brandPanel);
            sidePanel.add(Box.createVerticalStrut(20));

            // Navigation buttons with permission checks
            if (rolePermission.canAccessDashboard()) addNavButton(sidePanel, "Dashboard", "MainMenu");
            if (rolePermission.canAccessEmployee()) addNavButton(sidePanel, "Employee Management", "EmployeeList");
            if (rolePermission.canAccessPayroll()) addNavButton(sidePanel, "Payroll Management", "PayrollManagement");
            if (rolePermission.canAccessReports()) addNavButton(sidePanel, "Reports", "Reports");
            if (rolePermission.canAccessLeave()) addNavButton(sidePanel, "Leave Request", "LeavePanel");

            sidePanel.add(Box.createVerticalGlue());

            // Logout button
            JButton logoutBtn = AppUtils.createDangerButton("🚪 Logout");
            logoutBtn.addActionListener(e -> logout());
            JPanel logoutPanel = new JPanel(new FlowLayout());
            logoutPanel.setBackground(AppConstants.SIDEBAR_BACKGROUND);
            logoutPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));
            logoutPanel.add(logoutBtn);
            sidePanel.add(logoutPanel);

            return sidePanel;
        }

        private void addNavButton(JPanel parent, String text, String panelName) {
            JButton button = AppUtils.createNavigationButton(text);
            button.setPreferredSize(new Dimension(240, 45));
            button.addActionListener(e -> showPanel(panelName));

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.setBackground(AppConstants.SIDEBAR_BACKGROUND);
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            buttonPanel.add(button);

            parent.add(buttonPanel);
        }

        public void showPanel(String panelName) {
            // Role-based access enforcement
            switch (panelName) {
                case "MainMenu":
                    if (!rolePermission.canAccessDashboard()) return;
                    break;
                case "EmployeeList":
                    if (!rolePermission.canAccessEmployee()) return;
                    break;
                case "PayrollManagement":
                    if (!rolePermission.canAccessPayroll()) return;
                    break;
                case "Reports":
                    if (!rolePermission.canAccessReports()) return;
                    break;
                case "LeavePanel":
                    if (!rolePermission.canAccessLeave()) return;
                    break;
            }

            cardLayout.show(cardPanel, panelName);
        }

        // ✅ Convenience methods for old panel code (fix compilation errors)
        public void showMainMenu() { showPanel("MainMenu"); }
        public void showEmployeeManagement() { showPanel("EmployeeList"); }
        public void showPayrollManagement() { showPanel("PayrollManagement"); }
        public void showReports() { showPanel("Reports"); }
        public void showLeavePanel() { showPanel("LeavePanel"); }

        public void logout() {
            com.motorph.controller.AuthenticationController authController = new com.motorph.controller.AuthenticationController();
            boolean logoutSuccess = authController.logout();

            if (logoutSuccess) {
                this.dispose();
                System.exit(0);
            }
        }
    }