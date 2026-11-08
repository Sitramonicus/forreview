/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GUI;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 * @author MO-IT110 Group 8
 */
public class LogIn {
    //it will now display the login screen and ask for authentication.
    public static void showLoginScreen() {
        JFrame loginFrame = new JFrame("MotorPH Login");
        loginFrame.setSize(400, 250);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        JLabel userLabel = new JLabel("Username:");
        JTextField userText = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passText = new JPasswordField();
        JButton loginButton = new JButton("Login");
        
        //It adds the functionality to our login button.
        loginButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passText.getPassword());
   
            //Here is the authentication of the credentials.
            if (Authentication.authenticateUser(username, password)) {
                loginFrame.dispose();
                SwingUtilities.invokeLater(() -> {
                    MainMenu gui = new MainMenu(username);
                    gui.setVisible(true);
                });
            } else {
                //We also put error message if it doens't match a data.
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials, please try again.", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        //Adding components to our panel.
        panel.add(userLabel);
        panel.add(userText);
        panel.add(passLabel);
        panel.add(passText);
        panel.add(new JLabel());
        panel.add(loginButton);
        
        //Then we add the panel to our login frame and display it in the window.
        loginFrame.add(panel);
        loginFrame.setVisible(true);
    }
}