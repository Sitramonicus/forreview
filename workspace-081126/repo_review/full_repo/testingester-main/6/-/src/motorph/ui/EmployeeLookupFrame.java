package motorph.ui;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import motorph.model.Employee;
import motorph.service.CSVDataHandler;
import motorph.user.User;

/**
 * Employee-facing lookup screen. Lets a logged-in "employee" user enter their
 * own employee number and view their Employee Number, Employee Name, and
 * Birthday only — no other fields, no editing capability.
 */
public class EmployeeLookupFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(EmployeeLookupFrame.class.getName());
    private static final long serialVersionUID = 1L;
    private static final int FRAME_WIDTH = 500;
    private static final DateTimeFormatter BIRTHDAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final transient User currentUser;
    private final transient CSVDataHandler dataHandler = new CSVDataHandler();

    private JTextField employeeIdField;
    private JButton lookupButton;
    private JButton exitButton;
    private JLabel resultLabel;

    /**
     * @param currentUser the logged-in user; must have VIEW_OWN_RECORD to look up
     */
    @SuppressWarnings("this-escape")
    public EmployeeLookupFrame(User currentUser) {
        this.currentUser = currentUser;
        initComponents();
        setResizable(false);
        setSize(FRAME_WIDTH, 350);
        setupLayout();
        setLocationRelativeTo(null);
    }

    /**
     * Initializes Swing components and registers listeners only. No bounds or layout here.
     */
    private void initComponents() {
        setTitle("Employee Lookup");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        employeeIdField = new JTextField();
        lookupButton = new JButton("Look Up");
        lookupButton.setMnemonic('U');
        lookupButton.addActionListener(this::lookupButtonActionPerformed);
        resultLabel = new JLabel("");
        resultLabel.setVerticalAlignment(JLabel.TOP);
        exitButton = new JButton("Exit");
        exitButton.setMnemonic('X');
        exitButton.addActionListener(this::exitButtonActionPerformed);
    }

    /**
     * Adds components to panels, sets bounds, and applies visual styling. Called
     * after initComponents() to avoid performing layout during construction.
     */
    private void setupLayout() {
        getContentPane().setLayout(null);
        getContentPane().setBackground(new java.awt.Color(245, 245, 248));

        // Header panel
        javax.swing.JPanel headerPanel = new javax.swing.JPanel();
        headerPanel.setLayout(null);
        headerPanel.setBackground(new java.awt.Color(28, 57, 110));
        headerPanel.setBounds(0, 0, FRAME_WIDTH, 70);
        javax.swing.JLabel headerTitle = new javax.swing.JLabel("Employee Lookup");
        headerTitle.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 22));
        headerTitle.setForeground(new java.awt.Color(255, 255, 255));
        headerTitle.setBounds(16, 12, 300, 30);
        headerPanel.add(headerTitle);
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

        // Card panel for the lookup form
        javax.swing.JPanel cardPanel = new javax.swing.JPanel();
        cardPanel.setLayout(null);
        cardPanel.setBackground(new java.awt.Color(255, 255, 255));
        cardPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 210), 1));
        cardPanel.setBounds(50, 90, 400, 210);
        getContentPane().add(cardPanel);

        // Title inside card
        JLabel title = new JLabel("Enter Your Employee Number");
        title.setBounds(20, 20, 300, 25);
        title.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 13));
        title.setForeground(new java.awt.Color(30,30,50));
        cardPanel.add(title);

        // Move components into the card panel and adjust bounds
        employeeIdField.setBounds(20, 60, 200, 28);
        cardPanel.add(employeeIdField);

        lookupButton.setBounds(230, 60, 110, 28);
        cardPanel.add(lookupButton);

        resultLabel.setBounds(20, 100, 360, 70);
        cardPanel.add(resultLabel);

        exitButton.setBounds(20, 170, 100, 28);
        cardPanel.add(exitButton);

        // Apply styles
        applyFieldStyle(employeeIdField);
        applyButtonStyle(lookupButton);
        applySecondaryButtonStyle(exitButton);
    }

    private void lookupButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (!currentUser.hasPermission(User.VIEW_OWN_RECORD)) {
            JOptionPane.showMessageDialog(this, "Your account is not allowed to do this.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String id = employeeIdField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an employee number.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Employee employee;
        try {
            employee = dataHandler.findEmployeeById(id);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load employee data", e);
            JOptionPane.showMessageDialog(this,
                    "Unable to load employee data. Please check that the file exists at: " + CSVDataHandler.DEFAULT_EMPLOYEE_CSV,
                    "File Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (employee == null) {
            resultLabel.setText("<html>Employee number does not exist.</html>");
            return;
        }

        String html = "<html>Employee Number: " + employee.getEmployeeId()
                + "<br>Employee Name: " + employee.getFullName()
                + "<br>Birthday: " + employee.getBirthday().format(BIRTHDAY_FORMAT) + "</html>";
        resultLabel.setText(html);
    }

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {
        new LoginFrame().setVisible(true);
        this.dispose();
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
     * Applies standard styling to a text input field (JTextField or subclasses).
     *
     * @param field the JTextField to style
     */
    private void applyFieldStyle(javax.swing.JTextField field) {
        UIStyleHelper.applyFieldStyle(field);
    }
}
