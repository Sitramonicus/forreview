package com.motorph.view;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.motorph.controller.AuthenticationController;
import com.motorph.util.AppConstants;

/**
 * Login panel for the MotorPH Payroll System with background image.
 */
public class LoginPanel extends JPanel {
    private final AuthenticationController authController;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton exitButton;
    private JLabel statusLabel;
    private final LoginCallback callback;
    private BufferedImage backgroundImage;

    // Interface for login callback
    public interface LoginCallback {
        void onLoginSuccess();
        void onExit();
    }

    // Constructor
    public LoginPanel(AuthenticationController authController, LoginCallback callback) {
        this.authController = authController;
        this.callback = callback;

        // Load background image
        try {
            backgroundImage = ImageIO.read(getClass().getResource("/images/BackgroundMotorPH.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Background image not found!");
        }

        initPanel();
    }

    // Initialize panel
    private void initPanel() {
        setLayout(new BorderLayout());
        setOpaque(false); // make panel transparent for background

        // Main content panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;

        // Title
        JLabel titleLabel = new JLabel("MotorPH Payroll System", SwingConstants.CENTER);
        titleLabel.setFont(AppConstants.TITLE_FONT);
        titleLabel.setForeground(Color.WHITE); // change color for readability
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        mainPanel.add(titleLabel, gbc);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Please login to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(AppConstants.NORMAL_FONT);
        subtitleLabel.setForeground(Color.WHITE); // change color for readability
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 30, 0);
        mainPanel.add(subtitleLabel, gbc);

        // Form panel
        JPanel formPanel = createFormPanel();
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(formPanel, gbc);

        // Status label
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(AppConstants.SMALL_FONT);
        statusLabel.setForeground(AppConstants.DELETE_BUTTON_COLOR);
        gbc.gridy = 3;
        mainPanel.add(statusLabel, gbc);

        // Buttons
        JPanel buttonPanel = createButtonPanel();
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Focus on username field
        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());
    }

    // Create login form panel
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        // Username label
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(AppConstants.NORMAL_FONT);
        usernameLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        formPanel.add(usernameLabel, gbc);

        // Username field
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usernameField.setPreferredSize(new Dimension(350, 45));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        usernameField.addKeyListener(new EnterKeyListener());
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(usernameField, gbc);

        // Password label
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(AppConstants.NORMAL_FONT);
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 5, 0);
        formPanel.add(passwordLabel, gbc);

        // Password field
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        passwordField.setPreferredSize(new Dimension(350, 45));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        passwordField.addKeyListener(new EnterKeyListener());
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(passwordField, gbc);

        return formPanel;
    }

    // Create button panel
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        loginButton = new JButton("Login");
        loginButton.setFont(AppConstants.BUTTON_FONT);
        loginButton.setBackground(AppConstants.BUTTON_COLOR);
        loginButton.setForeground(Color.WHITE);
        loginButton.setPreferredSize(new Dimension(120, AppConstants.BUTTON_HEIGHT));
        loginButton.setFocusPainted(false);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> performLogin());
        loginButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { loginButton.setBackground(AppConstants.BUTTON_HOVER_COLOR); }
            @Override public void mouseExited(MouseEvent e) { loginButton.setBackground(AppConstants.BUTTON_COLOR); }
        });

        exitButton = new JButton("Exit");
        exitButton.setFont(AppConstants.BUTTON_FONT);
        exitButton.setBackground(AppConstants.SECONDARY_BUTTON_COLOR);
        exitButton.setForeground(Color.WHITE);
        exitButton.setPreferredSize(new Dimension(120, AppConstants.BUTTON_HEIGHT));
        exitButton.setFocusPainted(false);
        exitButton.setOpaque(true);
        exitButton.setBorderPainted(false);
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> performExit());
        exitButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { exitButton.setBackground(AppConstants.SECONDARY_BUTTON_HOVER); }
            @Override public void mouseExited(MouseEvent e) { exitButton.setBackground(AppConstants.SECONDARY_BUTTON_COLOR); }
        });

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        return buttonPanel;
    }

    // Login logic
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        statusLabel.setText(" ");

        if (username.isEmpty()) { statusLabel.setText("Please enter your username"); return; }
        if (password.isEmpty()) { statusLabel.setText("Please enter your password"); return; }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        try {
            if (authController.login(username, password)) {
                statusLabel.setText("Login successful!");
                passwordField.setText("");
                if (callback != null) callback.onLoginSuccess();
            } else {
                statusLabel.setText("Invalid username or password");
                passwordField.setText("");
            }
        } catch (Exception e) {
            statusLabel.setText("Login error: " + e.getMessage());
        } finally {
            loginButton.setEnabled(true);
            loginButton.setText("Login");
        }
    }

    // Exit logic
    private void performExit() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?",
                "Confirm Exit", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION && callback != null) callback.onExit();
    }

    // Enter key listener
    private class EnterKeyListener implements KeyListener {
        @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) performLogin(); }
        @Override public void keyTyped(KeyEvent e) {}
        @Override public void keyReleased(KeyEvent e) {}
    }

    // --- Methods needed by Login.java ---
    public void setFocusToUsername() { usernameField.requestFocusInWindow(); }
    public void clearForm() { usernameField.setText(""); passwordField.setText(""); statusLabel.setText(" "); usernameField.requestFocusInWindow(); }
}


