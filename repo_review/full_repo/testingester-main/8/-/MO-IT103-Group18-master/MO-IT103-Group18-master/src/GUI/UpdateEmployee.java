/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GUI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author MO-IT110 Group 8
 */
public class UpdateEmployee {
    //This method updates an employee records in the csv file while preserving the ID for look up
    //It also refreshes the table to show the updated data
    public static void updateEmployeeInCSV(String[] updatedEmployee, DefaultTableModel model) {
        List<String[]> employees = new ArrayList<>();
        boolean employeeUpdated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(ViewEmployee.EMPLOYEE_DETAILS_CSV))) {
            String header = br.readLine();
            employees.add(ViewEmployee.parseCSVLine(header));

            String line;
            //A while loop to process each records line by line
            while ((line = br.readLine()) != null) {
                String[] record = ViewEmployee.parseCSVLine(line);//Proper parsing
                if (record.length >= employees.get(0).length) {
                    if (record[0].equals(updatedEmployee[0])) { //Compare using the ID
                        for (int i = 0; i < updatedEmployee.length; i++) {
                            updatedEmployee[i] = removeQuotation(updatedEmployee[i]); //Ensure CSV formatting
                        }
                        
                        record = updatedEmployee; //Replace the whole record
                        employeeUpdated = true;
                    }
                    employees.add(record);
                } else {
                    //an error message if a record is malformed or has missing fields
                    System.err.println("Skipping malformed record: " + line);
                }
            }
        } catch (IOException e) {
            //If the program has an error while reading the file
            JOptionPane.showMessageDialog(null, "Error reading employee records!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!employeeUpdated) {
            //This will throw an error is the ID is not found or matched.
            JOptionPane.showMessageDialog(null, "Error: Employee record not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //This will rewrite the csv file with the updated employee rocords
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ViewEmployee.EMPLOYEE_DETAILS_CSV))) {
            for (String[] employee : employees) {
                bw.write(String.join(",", employee));
                bw.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving updated employee details!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        RefreshTable.refreshEmployeeTable(model, updatedEmployee[0], updatedEmployee); //Use NEW ID for refresh
        
    }
    public static String removeQuotation(String input) {
        input = input.replaceAll("^\"|\"$", ""); //it removes redundant quotes
        if (input.contains(",") || input.contains("\"") || input.contains("'")) {
            return "\"" + input.replace("\"", "\"\"") + "\""; //Properly escapes quotes
        }
        return input.trim();
    }
}