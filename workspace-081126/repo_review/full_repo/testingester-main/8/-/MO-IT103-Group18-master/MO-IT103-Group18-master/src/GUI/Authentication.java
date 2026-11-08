/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GUI;

/**
 *
 * @author MO-IT110 Group 8
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class Authentication {
    //Here it checks the user's credentials through the csv file (Feature 4)
    //If match is found it will return a true argument, if not it's false.
    public static boolean authenticateUser(String employeeID, String lastName) {
        //Try
        //We make a new csv file for the credentials of employees (Feature 4)
        String loginCSV = Backend.PayrollDataLoader.EMPLOYEE_DETAILS_CSV;

        try (BufferedReader br = new BufferedReader(new FileReader(loginCSV))) {
            String line;
            boolean isFirstLine = true;
            
            //This is where it reads each lines in the csv file
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                String[] credentials = MainMenu.parseCSVLine(line);
                
                //This is where it checks if the prased data has enough fields and mathces the employee ID and last name
                if (credentials.length >= 2 &&
                    credentials[0].trim().equals(employeeID) &&
                    credentials[1].trim().equalsIgnoreCase(lastName)) { //We make it case sensitive
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
}