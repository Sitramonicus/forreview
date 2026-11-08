/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EmployeePayrollMph;
import java.sql.*;
import javax.swing.*;
import javax.swing.JOptionPane;


/**
 *
 * @author Cal
 */
public class myMotorPHdb {
    
    Connection conn = null;
    public static Connection java_myMotorPhdb(){
    
    try{
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection
        ("jdbc:sqlite:C:\\Users\\Cal\\Documents\\NetBeansProjects\\EmployeePayrollMph\\myMotorPhdb.db");
        return conn;
           
    }catch (Exception e){
    JOptionPane.showMessageDialog(null, e);
    return null;
    
    }
    }
}
