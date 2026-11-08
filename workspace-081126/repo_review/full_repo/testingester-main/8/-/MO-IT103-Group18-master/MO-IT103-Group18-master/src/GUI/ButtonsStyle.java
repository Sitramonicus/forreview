/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GUI;

/**
 *
 * @author MO-IT110 Group 8
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;


public class ButtonsStyle {
    //We make two buttons to style each button from admin access window to the the employee table
    //This is our custon button design for button 1
    public static JButton ButtonStyle1(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setBackground(new Color(45, 45, 45));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        button.setPreferredSize(new Dimension(250, 50));
        return button;
    }
    
    //This is our custom button smaller buttons
    public static JButton ButtonStyle2(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(new Color(80, 80, 80));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        button.setPreferredSize(new Dimension(150, 30));
        return button;
    }
}