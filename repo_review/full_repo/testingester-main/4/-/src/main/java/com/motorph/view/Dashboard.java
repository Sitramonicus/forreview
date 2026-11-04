package com.motorph.view;

import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.swing.*;

import com.motorph.controller.EmployeeController;
import com.motorph.model.Employee;
import com.motorph.util.AppConstants;
import com.motorph.util.AppUtils;
import com.motorph.security.RolePermission;

public class Dashboard extends JPanel {

    private final MainFrame mainFrame;
    private final EmployeeController employeeController;
    private final RolePermission rolePermission; // ✅ Add RolePermission

    public Dashboard(MainFrame mainFrame, EmployeeController employeeController) {
        this.mainFrame = mainFrame;
        this.employeeController = employeeController;
        this.rolePermission = new RolePermission(AppUtils.getCurrentUser().getRole()); // Initialize
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND_COLOR);

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(AppConstants.BACKGROUND_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header
        JPanel headerPanel = createHeaderPanel();
        container.add(headerPanel, BorderLayout.NORTH);

        // Main content
        JPanel contentPanel = createMainContentPanel();
        container.add(contentPanel, BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppConstants.BACKGROUND_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        JLabel titleLabel = new JLabel("Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(AppConstants.TEXT_COLOR);

        header.add(titleLabel, BorderLayout.WEST);

        return header;
    }

    private JPanel createMainContentPanel() {
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(AppConstants.BACKGROUND_COLOR);

        // Statistics cards
        JPanel statsPanel = createStatisticsPanel();
        mainContent.add(statsPanel, BorderLayout.NORTH);

        // Quick links
        JPanel quickLinksPanel = createQuickLinksPanel();
        mainContent.add(quickLinksPanel, BorderLayout.CENTER);

        return mainContent;
    }

    private JPanel createStatisticsPanel() {
        JPanel statsContainer = new JPanel(new GridLayout(1, 4, 20, 0));
        statsContainer.setBackground(AppConstants.BACKGROUND_COLOR);

        List<Employee> allEmployees = employeeController.getAllEmployees();
        int totalEmployees = allEmployees.size();
        int activeEmployees = allEmployees.size(); // simplified
        double avgSalary = allEmployees.stream().mapToDouble(Employee::getBasicSalary).average().orElse(0);
        int recentHires = 3; // placeholder

        statsContainer.add(createStatCard("Total Employees", String.valueOf(totalEmployees)));
        statsContainer.add(createStatCard("Active Employees", String.valueOf(activeEmployees)));

        if (rolePermission.canAccessPayroll()) {
            statsContainer.add(createStatCard("Avg Monthly Salary", String.format("₱%.0f", avgSalary)));
        }
        if (rolePermission.canAccessEmployee()) {
            statsContainer.add(createStatCard("New Hires (This Month)", String.valueOf(recentHires)));
        }

        return statsContainer;
    }

    private JPanel createQuickLinksPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(AppConstants.BACKGROUND_COLOR);

        JLabel headerLabel = new JLabel("Quick Links");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerLabel.setForeground(AppConstants.TEXT_COLOR);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel linksPanel = new JPanel();
        linksPanel.setLayout(new BoxLayout(linksPanel, BoxLayout.Y_AXIS));
        linksPanel.setBackground(Color.WHITE);
        linksPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));

        // ✅ Quick links with role checks
        if (rolePermission.canAccessEmployee()) {
            linksPanel.add(createQuickLinkItem("Add New Employee", () -> mainFrame.showPanel("EmployeeList")));
            linksPanel.add(Box.createVerticalStrut(16));
        }
        if (rolePermission.canAccessPayroll()) {
            linksPanel.add(createQuickLinkItem("Process Payroll", () -> mainFrame.showPanel("PayrollManagement")));
            linksPanel.add(Box.createVerticalStrut(16));
        }
        if (rolePermission.canAccessReports()) {
            linksPanel.add(createQuickLinkItem("Generate Reports", () -> mainFrame.showPanel("Reports")));
        }

        container.add(headerLabel, BorderLayout.NORTH);
        container.add(linksPanel, BorderLayout.CENTER);

        return container;
    }

    private JPanel createQuickLinkItem(String title, Runnable action) {
        JPanel item = new JPanel(new BorderLayout());
        item.setOpaque(false);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(AppConstants.TEXT_COLOR);
        item.add(titleLabel, BorderLayout.CENTER);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (action != null) action.run();
            }
        });

        return item;
    }

    private JPanel createStatCard(String label, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(24, 20, 24, 20)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel labelLabel = new JLabel("<html>" + label.replace("\n", "<br>") + "</html>");
        labelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelLabel.setForeground(AppConstants.TEXT_SECONDARY);

        card.add(valueLabel, BorderLayout.NORTH);
        card.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        card.add(labelLabel, BorderLayout.SOUTH);

        return card;
    }
}