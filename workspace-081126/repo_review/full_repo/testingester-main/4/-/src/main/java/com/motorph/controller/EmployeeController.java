package com.motorph.controller;

import java.time.LocalDate;
import java.util.List;

import com.motorph.model.AttendanceRecord;
import com.motorph.model.Employee;
import com.motorph.service.EmployeeService;

public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public Employee findEmployeeById(int employeeId) {
        if (employeeId <= 0) throw new IllegalArgumentException("Invalid employee ID");
        Employee emp = employeeService.findEmployeeById(employeeId);
        if (emp == null) throw new IllegalArgumentException("Employee not found");
        return emp;
    }

    public List<Employee> searchEmployees(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) throw new IllegalArgumentException("Search term cannot be empty");
        return employeeService.searchEmployees(searchTerm);
    }

    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    public boolean addEmployee(Employee employee) {
        return employeeService.addEmployee(employee);
    }

    public boolean updateEmployee(Employee employee) {
        return employeeService.updateEmployee(employee);
    }

    public boolean deleteEmployee(int employeeId) {
        return employeeService.deleteEmployee(employeeId);
    }

    public List<AttendanceRecord> getAttendanceRecords(int employeeId, LocalDate startDate, LocalDate endDate) {
        return employeeService.getAttendanceRecords(employeeId, startDate, endDate);
    }

    public List<AttendanceRecord> getAllAttendanceRecords() {
        return employeeService.getAllAttendanceRecords();
    }

    public List<AttendanceRecord> getAttendanceRecordsForDate(LocalDate date) {
        return employeeService.getAttendanceRecordsForDate(date);
    }

    public List<AttendanceRecord> getAttendanceRecordsInRange(LocalDate startDate, LocalDate endDate) {
        return employeeService.getAttendanceRecordsInRange(startDate, endDate);
    }
}