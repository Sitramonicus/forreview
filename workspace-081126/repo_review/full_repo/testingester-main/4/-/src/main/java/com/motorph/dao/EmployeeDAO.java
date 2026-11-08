package com.motorph.dao;

import com.motorph.model.Employee;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    private static final String FILE_PATH = "src/main/resources/employees.csv";

    // READ ALL EMPLOYEES FROM CSV
    public List<Employee> getAllEmployees() {

        List<Employee> employees = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {

            String line;

            // Skip CSV header
            br.readLine();

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");

                Employee emp = new Employee();

                emp.setEmployeeId(Integer.parseInt(data[0]));
                emp.setLastName(data[1]);
                emp.setFirstName(data[2]);
                emp.setPosition(data[3]);
                emp.setStatus(data[4]);
                emp.setBasicSalary(Double.parseDouble(data[5]));
                emp.setRiceSubsidy(Double.parseDouble(data[6]));
                emp.setPhoneAllowance(Double.parseDouble(data[7]));
                emp.setClothingAllowance(Double.parseDouble(data[8]));

                employees.add(emp);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return employees;
    }
}