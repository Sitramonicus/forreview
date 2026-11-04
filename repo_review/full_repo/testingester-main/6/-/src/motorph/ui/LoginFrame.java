    /*
    * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
    * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
    */
    package motorph.ui;

    import java.util.List;
    import java.util.logging.Logger;
    import motorph.user.EmployeeUser;
    import motorph.user.PayrollStaff;
    import motorph.user.User;

    /**
     * Login screen for MotorPH Employee App.
     * Each account is a User object; the next screen is chosen by the user's
     * permissions (polymorphic hasPermission), not by checking the username.
     */
    public class LoginFrame extends javax.swing.JFrame {
        private static final Logger logger = Logger.getLogger(LoginFrame.class.getName());
        private static final long serialVersionUID = 1L;
        private static final int FRAME_WIDTH = 500;

        // System accounts. Credentials are private fields inside each User object.
        private static final List<User> ACCOUNTS = List.of(
            new EmployeeUser("employee", "motorph123"),
            new PayrollStaff("payroll_staff", "motorph123")
        );

        /**
         * Creates new form LoginFrame
         */
        @SuppressWarnings("this-escape")
        public LoginFrame() {
            initComponents();
            setSize(FRAME_WIDTH, 400);
            setupLayout();
            this.setLocationRelativeTo(null);
            this.setResizable(false);
        }

        /**
         * Initializes Swing components and registers listeners. This method must not
         * perform layout, set bounds, or apply styling. Those responsibilities are
         * handled in setupLayout().
         */
        private void initComponents() {
            motorph = new javax.swing.JLabel();
            password = new javax.swing.JLabel();
            username = new javax.swing.JLabel();
            usernameField = new javax.swing.JTextField();
            passwordField = new javax.swing.JPasswordField();
            loginButton = new javax.swing.JButton();

            setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

            // Register action listeners only – no layout, bounds or styling here.
            usernameField.addActionListener(this::usernameFieldActionPerformed);
            passwordField.addActionListener(this::passwordFieldActionPerformed);
            loginButton.setText("Login");
            loginButton.setMnemonic('L');
            loginButton.addActionListener(this::loginButtonActionPerformed);

            // Set basic label texts
            motorph.setText("MotorPH");
            username.setText("Username");
            password.setText("Password");
        }

        /**
         * Sets up all layout, bounds, and visual styling for the frame components.
         * <p>
         * This method performs all getContentPane().add(...), setBounds(...), color
         * and font assignments. It is called after initComponents() from the
         * constructor so that UI rendering is deterministic and does not depend on
         * getWidth() being available during construction.
         * </p>
         */
        private void setupLayout() {
            // Root layout
            getContentPane().setLayout(null);
            getContentPane().setBackground(new java.awt.Color(210, 225, 245));

            // Header panel
            javax.swing.JPanel headerPanel = new javax.swing.JPanel();
            headerPanel.setLayout(null);
            headerPanel.setBackground(new java.awt.Color(28, 57, 110));
            headerPanel.setBounds(0, 0, FRAME_WIDTH, 70);
            javax.swing.JLabel headerTitle = new javax.swing.JLabel("MotorPH");
            headerTitle.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 22));
            headerTitle.setForeground(new java.awt.Color(255, 255, 255));
            headerTitle.setBounds(16, 12, 200, 30);
            headerPanel.add(headerTitle);
            javax.swing.JLabel headerSubtitle = new javax.swing.JLabel("MotorPH Payroll System");
            headerSubtitle.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 12));
            headerSubtitle.setForeground(new java.awt.Color(210, 225, 245));
            headerSubtitle.setBounds(16, 40, 200, 16);
            headerPanel.add(headerSubtitle);
            getContentPane().add(headerPanel);

            // Separator
            javax.swing.JLabel separator = new javax.swing.JLabel();
            separator.setOpaque(true);
            separator.setBackground(new java.awt.Color(44, 90, 160));
            separator.setBounds(0, 70, FRAME_WIDTH, 2);
            getContentPane().add(separator);

            // Card panel to center the login form
            javax.swing.JPanel cardPanel = new javax.swing.JPanel();
            cardPanel.setLayout(null);
            cardPanel.setBackground(new java.awt.Color(255, 255, 255));
            cardPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 210), 1));
            cardPanel.setBounds(100, 85, 300, 270);
            getContentPane().add(cardPanel);

            // MotorPH title inside card
            motorph.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 28));
            motorph.setForeground(new java.awt.Color(28, 57, 110));
            motorph.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            motorph.setBounds(50, 10, 200, 50);
            cardPanel.add(motorph);

            javax.swing.JLabel subtitle = new javax.swing.JLabel();
            subtitle.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 12));
            subtitle.setForeground(new java.awt.Color(100, 100, 110));
            subtitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            subtitle.setText("Payroll Management System");
            subtitle.setBounds(50, 50, 200, 15);
            cardPanel.add(subtitle);

            username.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 13));
            username.setBounds(20, 80, 80, 25);
            username.setForeground(new java.awt.Color(30, 30, 50));
            cardPanel.add(username);

            usernameField.setBounds(new java.awt.Rectangle(20, 105, 260, 30));
            usernameField.setPreferredSize(new java.awt.Dimension(260, 30));
            cardPanel.add(usernameField);

            password.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 13));
            password.setBounds(20, 145, 80, 25);
            password.setForeground(new java.awt.Color(30, 30, 50));
            cardPanel.add(password);

            passwordField.setBounds(new java.awt.Rectangle(20, 170, 260, 30));
            passwordField.setPreferredSize(new java.awt.Dimension(260, 30));
            cardPanel.add(passwordField);

            loginButton.setBounds(new java.awt.Rectangle(20, 215, 260, 35));
            loginButton.setPreferredSize(new java.awt.Dimension(260, 35));
            cardPanel.add(loginButton);

            // Apply styles
            applyFieldStyle(usernameField);
            applyFieldStyle(passwordField);
            applyButtonStyle(loginButton);
        }

        //NAVIGATION
        /**
         * Opens the screen the logged-in user is allowed to use. The User subclass
         * (EmployeeUser or PayrollStaff) answers hasPermission().
         *
         * @param user the user who just logged in
         */
        private void openRoleBasedScreen(User user){
            if (user.hasPermission(User.VIEW_EMPLOYEES)) {
                new DashboardFrame(user).setVisible(true);
            } else if (user.hasPermission(User.VIEW_OWN_RECORD)) {
                new EmployeeLookupFrame(user).setVisible(true);
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "This account has no screens it can open.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            this.dispose();
        }

        //EVENT HANDLERS
        private void passwordFieldActionPerformed(java.awt.event.ActionEvent evt){
            loginButtonActionPerformed(evt);
        }
    
        private void usernameFieldActionPerformed(java.awt.event.ActionEvent evt){
            loginButtonActionPerformed(evt);
        }

        private void loginButtonActionPerformed(java.awt.event.ActionEvent evt) {
            String inputUsername = usernameField.getText().trim();
            String inputPassword = new String(passwordField.getPassword()).trim();

            if(inputUsername.isEmpty() || inputPassword.isEmpty()){
                javax.swing.JOptionPane.showMessageDialog(this, "Please enter your username or password.", "Empty Fields", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            User user = findAuthenticatedUser(inputUsername, inputPassword);
            if(user != null){
                openRoleBasedScreen(user);
            }
            else{
                javax.swing.JOptionPane.showMessageDialog(this, "Invalid Username or Password", "Login Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        }

        //VALIDATION
        /** Returns the account whose authenticate() accepts these credentials, or null. */
        private User findAuthenticatedUser(String inputUsername, String inputPassword){
            for (User account : ACCOUNTS) {
                if (account.authenticate(inputUsername, inputPassword)) {
                    return account;
                }
            }
            return null;
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
         * Applies standard styling to a text input field (JTextField or subclasses).
         *
         * @param field the JTextField to style
         */
        private void applyFieldStyle(javax.swing.JTextField field) {
            UIStyleHelper.applyFieldStyle(field);
        }

        /**
         * @param args the command line arguments
         */
        public static void main(String args[]) {
            /* Set the Nimbus look and feel */
            //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
            /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
            * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
            */
            try {
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
                logger.log(java.util.logging.Level.SEVERE, null, ex);
            }
            //</editor-fold>

            /* Create and display the form */
            java.awt.EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
        }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton loginButton;
        private javax.swing.JLabel motorph;
        private javax.swing.JLabel password;
        private javax.swing.JPasswordField passwordField;
        private javax.swing.JLabel username;
        private javax.swing.JTextField usernameField;
        // End of variables declaration//GEN-END:variables
    }
