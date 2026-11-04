package Backend;

// Concrete class with no subclasses (MS1 Week 4): employmentStatus ("Regular" or
// "Probationary") is a plain attribute because both compute salary the same way.
public class Employee {
    private final String employeeId;      // read-only, no setter (MS1 Method Dictionary)
    private String lastName;
    private String firstName;
    private String position;
    private String employmentStatus;
    private double basicSalary;
    private double hourlyRate;
    private double riceSubsidy;
    private double phoneAllowance;
    private double clothingAllowance;

    public Employee(String employeeId, String lastName, String firstName, String position,
                    String employmentStatus, double basicSalary, double hourlyRate,
                    double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        this.employeeId = employeeId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.position = position;
        this.employmentStatus = employmentStatus;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
        setBasicSalary(basicSalary);   // reuse setter validation
        setHourlyRate(hourlyRate);
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public String getPosition() {
        return position;
    }

    // Read-only; returns "Regular" or "Probationary"
    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    // Total of Rice Subsidy + Phone Allowance + Clothing Allowance
    public double getAllowances() {
        return riceSubsidy + phoneAllowance + clothingAllowance;
    }

    // Validation rule: salary must be > 0; invalid values are rejected
    public void setBasicSalary(double salary) {
        if (salary <= 0) {
            throw new IllegalArgumentException("Basic salary must be greater than 0.");
        }
        this.basicSalary = salary;
    }

    // Validation rule: rate must be > 0
    public void setHourlyRate(double rate) {
        if (rate <= 0) {
            throw new IllegalArgumentException("Hourly rate must be greater than 0.");
        }
        this.hourlyRate = rate;
    }

    // One shared, concrete salary formula for every employee (hourlyRate x hours worked).
    // Same formula as CP2's computeSalary(). Employee does not store hours, so the
    // caller (PayrollProcessor) passes in the hours from AttendanceRecord.
    public double calculateSalary(double hoursWorked) {
        if (hoursWorked < 0) {
            throw new IllegalArgumentException("Hours worked cannot be negative.");
        }
        return hoursWorked * hourlyRate;
    }

    public static void main(String[] args) {
        Employee emp1 = new Employee("10001", "Garcia", "Manuel III", "Chief Executive Officer",
                "Regular", 90000.0, 535.71, 1500.0, 2000.0, 1000.0);

        System.out.println("Employee ID: " + emp1.getEmployeeId());
        System.out.println("Employee Name: " + emp1.getName());
        System.out.println("Position: " + emp1.getPosition());
        System.out.println("Employment Status: " + emp1.getEmploymentStatus());
        System.out.println("Basic Salary: " + emp1.getBasicSalary());
        System.out.println("Hourly Rate: " + emp1.getHourlyRate());
        System.out.println("Total Allowances: " + emp1.getAllowances());
        System.out.println("Salary (160 hrs): " + emp1.calculateSalary(160));

        try {
            emp1.setHourlyRate(-50);
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected invalid rate: " + e.getMessage());
        }
    }
}