/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package motorph.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import motorph.model.Employee;
import motorph.service.CSVDataHandler;
import motorph.user.User;

/**
 * Employee management screen: list, search, view, add, edit, delete.
 * <p>
 * The frame only collects input and shows results. Validation happens in the
 * Employee setters (called through CSVDataHandler.createEmployee) and all file
 * reading and writing goes through CSVDataHandler.
 * </p>
 */
public class EmployeeFrame extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(EmployeeFrame.class.getName());

    private static final long serialVersionUID = 1L;

    private static final int FRAME_WIDTH = 900;
    private static final List<String> EMPLOYEE_FIELD_NAMES = CSVDataHandler.EMPLOYEE_FIELD_NAMES;

    private final transient User currentUser;
    private final transient CSVDataHandler dataHandler = new CSVDataHandler();
    private transient List<Employee> employees = new ArrayList<>();

    /**
     * @param currentUser the logged-in user; Add, Edit, and Delete need MANAGE_EMPLOYEES
     */
    @SuppressWarnings("this-escape")
    public EmployeeFrame(User currentUser) {
        this.currentUser = currentUser;
        initComponents();
        setResizable(false);
        setSize(FRAME_WIDTH, 600);
        setupLayout();
        loadEmployees();
        setLocationRelativeTo(null);
    }

    /**
     * Initializes component instances and registers listeners only. No bounds or layout.
     */
    private void initComponents() {

        employeeLabel = new javax.swing.JLabel();
        searchLabel = new javax.swing.JLabel();
        searchField = new javax.swing.JTextField();
        searchButton = new javax.swing.JButton();
        showAllButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        employeeTable = new javax.swing.JTable();
        viewButton = new javax.swing.JButton();
        backButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        employeeLabel.setText("Employee Management");

        searchLabel.setText("Search:");

        searchField.addActionListener(this::searchFieldActionPerformed);

        searchButton.setText("Search");
        searchButton.setMnemonic('S');
        searchButton.addActionListener(this::searchButtonActionPerformed);

        showAllButton.setText("Show All");
        showAllButton.setMnemonic('A');
        showAllButton.addActionListener(this::showAllButtonActionPerformed);

        employeeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Employee ID", "Last Name", "First Name", "Position", "Status"
            }
        ) {
            Class<?>[] types = new Class<?>[] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
                public boolean isCellEditable(int row, int column) {
                return false;}
        });
        jScrollPane1.setViewportView(employeeTable);

        viewButton.setText("View Details");
        viewButton.setMnemonic('V');
        viewButton.addActionListener(this::viewButtonActionPerformed);

        addButton.setText("Add Employee");
        addButton.setMnemonic('D');
        addButton.addActionListener(this::addButtonActionPerformed);

        editButton.setText("Edit");
        editButton.setMnemonic('E');
        editButton.addActionListener(this::editButtonActionPerformed);

        deleteButton.setText("Delete");
        deleteButton.setMnemonic('L');
        deleteButton.addActionListener(this::deleteButtonActionPerformed);

        backButton.setText("Back");
        backButton.setMnemonic('B');
        backButton.addActionListener(this::backButtonActionPerformed);
    }

    /**
     * Performs layout and styling for all components. Called after initComponents().
     */
    private void setupLayout() {
        getContentPane().setLayout(null);
        getContentPane().setBackground(new java.awt.Color(245, 245, 248));

        // Header panel
        javax.swing.JPanel headerPanel = new javax.swing.JPanel();
        headerPanel.setLayout(null);
        headerPanel.setBackground(new java.awt.Color(28, 57, 110));
        headerPanel.setBounds(0, 0, FRAME_WIDTH, 70);

        // Move title into the header
        employeeLabel.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 22));
        employeeLabel.setForeground(new java.awt.Color(255, 255, 255));
        employeeLabel.setBounds(16, 12, 300, 30);
        headerPanel.add(employeeLabel);

        javax.swing.JLabel headerSubtitle = new javax.swing.JLabel("MotorPH Payroll System");
        headerSubtitle.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 12));
        headerSubtitle.setForeground(new java.awt.Color(210, 225, 245));
        headerSubtitle.setBounds(16, 40, 300, 16);
        headerPanel.add(headerSubtitle);

        getContentPane().add(headerPanel);

        // Separator
        javax.swing.JLabel separator = new javax.swing.JLabel();
        separator.setOpaque(true);
        separator.setBackground(new java.awt.Color(44, 90, 160));
        separator.setBounds(0, 70, FRAME_WIDTH, 2);
        getContentPane().add(separator);

        // Search controls
        searchLabel.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 13));
        searchLabel.setForeground(new java.awt.Color(30, 30, 50));
        searchLabel.setBounds(20, 80, 50, 18);
        getContentPane().add(searchLabel);

        searchField.setBounds(90, 80, 200, 25);
        getContentPane().add(searchField);

        searchButton.setBounds(300, 80, 100, 25);
        getContentPane().add(searchButton);

        showAllButton.setBounds(410, 80, 110, 25);
        getContentPane().add(showAllButton);

        // Table and scroll
        jScrollPane1.setBounds(20, 110, 860, 380);
        getContentPane().add(jScrollPane1);

        // Bottom action buttons
        viewButton.setBounds(20, 510, 120, 30);
        addButton.setBounds(150, 510, 140, 30);
        editButton.setBounds(300, 510, 100, 30);
        deleteButton.setBounds(410, 510, 100, 30);
        backButton.setBounds(780, 510, 100, 30);

        getContentPane().add(viewButton);
        getContentPane().add(addButton);
        getContentPane().add(editButton);
        getContentPane().add(deleteButton);
        getContentPane().add(backButton);

        applyFieldStyle(searchField);
        applyButtonStyle(searchButton);
        applySecondaryButtonStyle(showAllButton);

        // Bottom buttons
        applyButtonStyle(viewButton);
        applyButtonStyle(addButton);
        applyButtonStyle(editButton);
        applyDangerButtonStyle(deleteButton);
        applySecondaryButtonStyle(backButton);

        // Only roles with MANAGE_EMPLOYEES may add, edit, or delete
        boolean canManage = currentUser.hasPermission(User.MANAGE_EMPLOYEES);
        addButton.setEnabled(canManage);
        editButton.setEnabled(canManage);
        deleteButton.setEnabled(canManage);

        // Table styling
        employeeTable.setRowHeight(28);
        employeeTable.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 12));
        employeeTable.setGridColor(new java.awt.Color(220, 220, 230));
        employeeTable.setSelectionBackground(new java.awt.Color(44, 90, 160));
        employeeTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        employeeTable.setShowHorizontalLines(true);
        employeeTable.setShowVerticalLines(false);
        employeeTable.getTableHeader().setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 12));
        employeeTable.getTableHeader().setBackground(new java.awt.Color(28, 57, 110));
        employeeTable.getTableHeader().setForeground(new java.awt.Color(255, 255, 255));
        employeeTable.getTableHeader().setReorderingAllowed(false);
        employeeTable.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 30));
    }

    // DATA FUNCTIONS
    /**
     * Loads employees through CSVDataHandler and repopulates the table.
     */
    private void loadEmployees() {
        setTitle("Employee Management — Loading...");
        try {
            employees = dataHandler.loadEmployees();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load employees", e);
            JOptionPane.showMessageDialog(this,
                "Error loading employee data. Please check the file path.",
                "Load Error",
                JOptionPane.ERROR_MESSAGE);
            employees = new ArrayList<>();
        }
        populateEmployeeTable(employees);
        setTitle("Employee Management");
    }

    private void populateEmployeeTable(List<Employee> data) {
        DefaultTableModel model = (DefaultTableModel) employeeTable.getModel();
        model.setRowCount(0);

        for (Employee employee : data) {
            model.addRow(new Object[]{
                employee.getEmployeeId(),
                employee.getLastName(),
                employee.getFirstName(),
                employee.getPosition(),
                employee.getStatus()
            });
        }
    }

    /**
     * Saves the in-memory list through CSVDataHandler.
     *
     * @return true on success; shows an error dialog and returns false otherwise
     */
    private boolean saveEmployees(java.awt.Component parent) {
        try {
            dataHandler.saveEmployees(employees);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to save employees", e);
            JOptionPane.showMessageDialog(parent, "Error saving employee records. Please try again.", "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Returns the Employee for the selected table row, or null after showing a warning. */
    private Employee getSelectedEmployee(String actionName) {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an employee to " + actionName + ".",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String employeeId = (String) employeeTable.getValueAt(selectedRow, 0);
        for (Employee employee : employees) {
            if (employee.getEmployeeId().equals(employeeId)) {
                return employee;
            }
        }
        JOptionPane.showMessageDialog(this, "Failed to locate the selected employee.", "Data Error", JOptionPane.ERROR_MESSAGE);
        return null;
    }

    // SEARCH FUNCTIONS
    private void searchEmployee() {
        String searchQuery = searchField.getText().trim().toLowerCase();

        // If search is empty, reset to full list instead of showing warning
        if (searchQuery.isEmpty()) {
            populateEmployeeTable(employees);
            return;
        }

        List<Employee> results = new ArrayList<>();
        for (Employee employee : employees) {
            if (employee.getEmployeeId().toLowerCase().contains(searchQuery)
                    || employee.getLastName().toLowerCase().contains(searchQuery)
                    || employee.getFirstName().toLowerCase().contains(searchQuery)) {
                results.add(employee);
            }
        }

        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No employees found for: " + searchQuery,
                "No Results",
                JOptionPane.INFORMATION_MESSAGE);
            populateEmployeeTable(employees);
        } else {
            populateEmployeeTable(results);
        }
    }

    // VIEW EMPLOYEE FUNCTIONS
    private void viewEmployee() {
        Employee employee = getSelectedEmployee("view");
        if (employee == null) return;

        String[] values = dataHandler.toFields(employee);
        String[][] detailData = new String[values.length][];
        for (int i = 0; i < values.length; i++) {
            detailData[i] = new String[] { EMPLOYEE_FIELD_NAMES.get(i), values[i] };
        }
        String[] columnNames = { "Field", "Value" };

        javax.swing.JTable detailTable = new javax.swing.JTable(detailData, columnNames);
        detailTable.setEnabled(false);
        detailTable.setRowHeight(24);
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(280);

        javax.swing.JScrollPane detailScrollPane = new javax.swing.JScrollPane(detailTable);
        detailScrollPane.setPreferredSize(new java.awt.Dimension(440, 420));

        JOptionPane.showMessageDialog(
            this,
            detailScrollPane,
            "Employee Details - " + employee.getFullName(),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // NAVIGATION FUNCTIONS
    private void openDashboard() {
        new DashboardFrame(currentUser).setVisible(true);
        this.dispose();
    }

    // EVENT HANDLERS
    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) { searchEmployee(); }
    private void viewButtonActionPerformed(java.awt.event.ActionEvent evt) { viewEmployee(); }
    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) { openDashboard(); }

    /**
     * Action handler for the Add Employee button.
     *
     * @param evt the action event
     */
    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        showAddEmployeeDialog();
    }

    /**
     * Shows a modal dialog that collects all employee fields. On Save,
     * CSVDataHandler.createEmployee() validates the values and builds a Regular
     * or Probationary employee; the list is then saved and the table refreshed.
     */
    private void showAddEmployeeDialog() {
        String nextId;
        try {
            nextId = dataHandler.generateNextEmployeeId();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to generate employee ID", e);
            JOptionPane.showMessageDialog(this, "Error reading employee data. Please try again.", "Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object[] parts = buildEmployeeFormDialog("Add Employee", null);
        JDialog dialog = (JDialog) parts[0];
        JTextField[] inputs = (JTextField[]) parts[1];
        JButton saveBtn = (JButton) parts[2];
        JButton cancelBtn = (JButton) parts[3];

        inputs[0].setText(nextId);
        inputs[0].setEditable(false);

        saveBtn.addActionListener((ActionEvent e) -> {
            Employee newEmployee = buildEmployeeFromForm(dialog, inputs);
            if (newEmployee == null) return;

            int confirm = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to add this employee?", "Confirm Add", JOptionPane.YES_NO_OPTION);
            if (confirm != javax.swing.JOptionPane.YES_OPTION) {
                // keep dialog open for user to make changes
                return;
            }

            employees.add(newEmployee);
            if (!saveEmployees(dialog)) {
                employees.remove(newEmployee);
                return;
            }

            JOptionPane.showMessageDialog(this, "Employee added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            populateEmployeeTable(employees);
            dialog.dispose();
        });

        cancelBtn.addActionListener((ActionEvent e) -> dialog.dispose());

        dialog.setVisible(true);
    }

    /**
     * Reads the form values and asks CSVDataHandler to build the employee.
     * Shows the validation message from the backend if a value is invalid.
     *
     * @return the new Employee, or null if a value was rejected
     */
    private Employee buildEmployeeFromForm(JDialog dialog, JTextField[] inputs) {
        String[] values = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            values[i] = inputs[i].getText().trim();
        }
        try {
            return dataHandler.createEmployee(values);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    /**
     * Builds and returns a modal employee form dialog with pre-filled values.
     * Used by both the Add and Edit flows to avoid code duplication.
     *
     * @param title   the dialog window title
     * @param prefill array of pre-filled values; null or empty strings show blank fields
     * @return array where [0] = JDialog, [1] = JTextField[] inputs, [2] = JButton saveBtn, [3] = JButton cancelBtn
     *         Caller adds ActionListeners to the Save and Cancel buttons via getComponent()
     */
    private Object[] buildEmployeeFormDialog(String title, String[] prefill) {
        JTextField[] inputs = new JTextField[EMPLOYEE_FIELD_NAMES.size()];
        JPanel formPanel = new JPanel(new GridLayout(EMPLOYEE_FIELD_NAMES.size(), 2, 6, 6));
        formPanel.setPreferredSize(new Dimension(400, EMPLOYEE_FIELD_NAMES.size() * 32));
        for (int i = 0; i < EMPLOYEE_FIELD_NAMES.size(); i++) {
            JLabel fieldLabel = new JLabel(EMPLOYEE_FIELD_NAMES.get(i) + ":");
            fieldLabel.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 12));
            fieldLabel.setForeground(new java.awt.Color(30, 30, 50));
            fieldLabel.setHorizontalAlignment(JLabel.RIGHT);
            formPanel.add(fieldLabel);
            String val = (prefill != null && i < prefill.length && prefill[i] != null) ? prefill[i] : "";
            inputs[i] = new JTextField(val);
            applyFieldStyle(inputs[i]);
            formPanel.add(inputs[i]);
        }
        inputs[EMPLOYEE_FIELD_NAMES.indexOf("Birthday")].setToolTipText("MM/DD/YYYY");
        inputs[EMPLOYEE_FIELD_NAMES.indexOf("Status")].setToolTipText("Regular or Probationary");

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setPreferredSize(new Dimension(420, Math.min(EMPLOYEE_FIELD_NAMES.size() * 32, 520)));

        JPanel buttonsPanel = new JPanel();
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        saveBtn.setMnemonic('S');
        cancelBtn.setMnemonic('C');
        applyButtonStyle(saveBtn);
        applyButtonStyle(cancelBtn);
        buttonsPanel.add(saveBtn);
        buttonsPanel.add(cancelBtn);

        JDialog dialog = new JDialog(this, title, true);
        dialog.getContentPane().setLayout(new java.awt.BorderLayout(8, 8));
        dialog.getContentPane().add(scrollPane, java.awt.BorderLayout.CENTER);
        dialog.getContentPane().add(buttonsPanel, java.awt.BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        // Add Enter key handler to the last text field to trigger Save
        if (inputs.length > 0) {
            final JTextField lastField = inputs[inputs.length - 1];
            lastField.addActionListener(e -> saveBtn.doClick());
        }

        // Add Escape key handler to the dialog's root pane to trigger Cancel
        javax.swing.KeyStroke escapeKeyStroke = javax.swing.KeyStroke.getKeyStroke("ESCAPE");
        dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(escapeKeyStroke, "closeDialog");
        dialog.getRootPane().getActionMap().put("closeDialog", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cancelBtn.doClick();
            }
        });

        return new Object[] { dialog, inputs, saveBtn, cancelBtn };
    }

    /**
     * Called when Enter is pressed in the search field to trigger the search.
     *
     * @param evt the action event
     */
    private void searchFieldActionPerformed(java.awt.event.ActionEvent evt) {
        searchEmployee();
    }

    private void showAllButtonActionPerformed(java.awt.event.ActionEvent evt) {
        searchField.setText("");
        populateEmployeeTable(employees);
    }

    /**
     * Action handler for the Edit button. The Employee ID is read-only (it has
     * no setter). On Save, a new object is built through CSVDataHandler, so a
     * status change produces the matching Regular or Probationary subclass.
     *
     * @param evt the action event
     */
    private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Employee original = getSelectedEmployee("edit");
        if (original == null) return;

        Object[] parts = buildEmployeeFormDialog("Edit Employee", dataHandler.toFields(original));
        JDialog dialog = (JDialog) parts[0];
        JTextField[] inputs = (JTextField[]) parts[1];
        JButton saveBtn = (JButton) parts[2];
        JButton cancelBtn = (JButton) parts[3];

        inputs[0].setEditable(false);

        saveBtn.addActionListener((ActionEvent e) -> {
            Employee updated = buildEmployeeFromForm(dialog, inputs);
            if (updated == null) return;

            int confirm = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to save changes?", "Confirm Save", JOptionPane.YES_NO_OPTION);
            if (confirm != javax.swing.JOptionPane.YES_OPTION) {
                return;
            }

            int index = employees.indexOf(original);
            if (index < 0) {
                JOptionPane.showMessageDialog(dialog, "Failed to locate original employee record.", "Update Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            employees.set(index, updated);
            if (!saveEmployees(dialog)) {
                employees.set(index, original);
                return;
            }

            JOptionPane.showMessageDialog(this, "Employee updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            populateEmployeeTable(employees);
            dialog.dispose();
        });

        cancelBtn.addActionListener((ActionEvent e) -> dialog.dispose());
        dialog.setVisible(true);
    }

    /**
     * Action handler for the Delete button. Prompts for confirmation and removes
     * the selected employee, then saves the list through CSVDataHandler.
     *
     * @param evt the action event
     */
    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Employee employee = getSelectedEmployee("delete");
        if (employee == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this employee? This action cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        int index = employees.indexOf(employee);
        employees.remove(index);
        if (!saveEmployees(this)) {
            employees.add(index, employee);
            return;
        }

        JOptionPane.showMessageDialog(this, "Employee deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        populateEmployeeTable(employees);
    }

    /**
     * Applies standard styling to a JButton according to the UI guidelines.
     *
     * @param button the JButton to style
     */
    private void applyButtonStyle(javax.swing.JButton button) {
        UIStyleHelper.applyButtonStyle(button);
    }

    /**
     * Applies secondary button styling (gray).
     *
     * @param button the JButton to style
     */
    private void applySecondaryButtonStyle(javax.swing.JButton button) {
        UIStyleHelper.applySecondaryButtonStyle(button);
    }

    /**
     * Applies danger button styling (red).
     *
     * @param button the JButton to style
     */
    private void applyDangerButtonStyle(javax.swing.JButton button) {
        UIStyleHelper.applyDangerButtonStyle(button);
    }

    /**
     * Applies standard styling to a text input field (JTextField or subclasses).
     *
     * @param field the JTextField to style
     */
    private void applyFieldStyle(javax.swing.JTextField field) {
        UIStyleHelper.applyFieldStyle(field);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JButton addButton;
    private javax.swing.JButton editButton;
    private javax.swing.JButton deleteButton;
    private javax.swing.JLabel employeeLabel;
    private javax.swing.JTable employeeTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton searchButton;
    private javax.swing.JButton showAllButton;
    private javax.swing.JTextField searchField;
    private javax.swing.JLabel searchLabel;
    private javax.swing.JButton viewButton;
    // End of variables declaration//GEN-END:variables
}
