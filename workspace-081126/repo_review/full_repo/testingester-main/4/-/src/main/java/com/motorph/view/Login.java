package com.motorph.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.motorph.controller.AuthenticationController;

/**
 * Login frame for the MotorPH Payroll System.
 * Implements MPHCR-04 requirement for user authentication.
 */
public class Login extends JFrame implements LoginPanel.LoginCallback {
    private static final Logger logger = Logger.getLogger(Login.class.getName());

    private final AuthenticationController authController;
    private LoginPanel loginPanel;
    private Runnable onLoginSuccessCallback;

    /**
     * Constructor for Login
     */
    public Login() {
        this.authController = new AuthenticationController();
        initFrame();
    }

    /**
     * Constructor with success callback
     *
     * @param onLoginSuccessCallback Callback to execute on successful login
     */
    public Login(Runnable onLoginSuccessCallback) {
        this.authController = new AuthenticationController();
        this.onLoginSuccessCallback = onLoginSuccessCallback;
        initFrame();
    }

    /**
     * Initialize the login frame
     */
    private void initFrame() {
        setTitle("MotorPH Payroll System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create login panel
        loginPanel = new LoginPanel(authController, this);

        // Wrapper panel to include logo and login panel
        JPanel wrapperPanel = new JPanel(new BorderLayout());

        // Load logo from resources
        URL logoUrl = getClass().getResource("/images/motorph-logo.png");
if (logoUrl != null) {
    ImageIcon logoIcon = new ImageIcon(logoUrl);
    Image scaledLogo = logoIcon.getImage().getScaledInstance(120, 50, Image.SCALE_SMOOTH);
    JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
    logoLabel.setHorizontalAlignment(JLabel.CENTER);

    JPanel logoPanel = new JPanel(new BorderLayout());
    logoPanel.setBackground(java.awt.Color.WHITE); // Set background to white
    logoPanel.add(logoLabel, BorderLayout.CENTER);
    logoPanel.setPreferredSize(new Dimension(500, 60)); // Adjust height as needed

    wrapperPanel.add(logoPanel, BorderLayout.NORTH);
} else {
    System.err.println("Logo not found at /images/motorph-logo.png");
}

        // Add login panel to center of wrapper
        wrapperPanel.add(loginPanel, BorderLayout.CENTER);

        // Add wrapper to frame
        add(wrapperPanel, BorderLayout.CENTER);

        // Set frame properties
        setSize(500, 600);
        setMinimumSize(new Dimension(450, 550));
        setLocationRelativeTo(null);
        setResizable(false);

        // Set window icon
        try {
            setIconImage(new ImageIcon(getClass().getResource("/images/motorph-logo.png")).getImage());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load application icon", e);
        }

        logger.log(Level.INFO, "Login frame initialized");
    }

    /**
     * Callback for successful login
     */
    @Override
    public void onLoginSuccess() {
        logger.log(Level.INFO, "Login successful, transitioning to main application");

        // Hide login frame
        setVisible(false);

        // Execute success callback if provided
        if (onLoginSuccessCallback != null) {
            SwingUtilities.invokeLater(onLoginSuccessCallback);
        }

        // Dispose of login frame
        dispose();
    }

    /**
     * Callback for exit action
     */
    @Override
    public void onExit() {
        logger.log(Level.INFO, "User chose to exit application");
        System.exit(0);
    }

    /**
     * Show the login frame
     */
    public void showLogin() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            loginPanel.setFocusToUsername();
        });
    }

    /**
     * Get the authentication controller
     *
     * @return The authentication controller
     */
    public AuthenticationController getAuthenticationController() {
        return authController;
    }

    /**
     * Clear the login form
     */
    public void clearLoginForm() {
        if (loginPanel != null) {
            loginPanel.clearForm();
        }
    }
}
