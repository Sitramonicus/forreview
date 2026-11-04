
package motorphpayrollsystem;

public abstract class Employee {

    private int employeeID;
    private String lastName;
    private String firstName;
    private String sssNumber;
    private String philHealthNumber;
    private String tinNumber;
    private String pagIbigNumber;
    private String status;
    private String immediateSupervisor;
    private String position;

    private double riceSubsidy;
    private double phoneAllowance;
    private double clothingAllowance;
    private double hourlyRate;

    public Employee(
            int employeeID,
            String lastName,
            String firstName,
            String sssNumber,
            String philHealthNumber,
            String tinNumber,
            String pagIbigNumber,
            String status,
            String immediateSupervisor,
            String position,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance,
            double hourlyRate) {

        setEmployeeID(employeeID);
        setLastName(lastName);
        setFirstName(firstName);
        setSssNumber(sssNumber);
        setPhilHealthNumber(philHealthNumber);
        setTinNumber(tinNumber);
        setPagIbigNumber(pagIbigNumber);
        setStatus(status);
        setImmediateSupervisor(immediateSupervisor);
        setPosition(position);
        setRiceSubsidy(riceSubsidy);
        setPhoneAllowance(phoneAllowance);
        setClothingAllowance(clothingAllowance);
        setHourlyRate(hourlyRate);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private static double requireNonnegative(double value, String field) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    field + " must be a nonnegative number.");
        }
        return value;
    }

    public int getEmployeeID() { return employeeID; }
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getSssNumber() { return sssNumber; }
    public String getPhilHealthNumber() { return philHealthNumber; }
    public String getTinNumber() { return tinNumber; }
    public String getPagIbigNumber() { return pagIbigNumber; }
    public String getStatus() { return status; }
    public String getImmediateSupervisor() { return immediateSupervisor; }
    public String getPosition() { return position; }
    public double getRiceSubsidy() { return riceSubsidy; }
    public double getPhoneAllowance() { return phoneAllowance; }
    public double getClothingAllowance() { return clothingAllowance; }
    public double getHourlyRate() { return hourlyRate; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void setEmployeeID(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Employee ID must be greater than zero.");
        }
        this.employeeID = id;
    }

    public void setLastName(String value) {
        lastName = requireText(value, "Last name");
    }

    public void setFirstName(String value) {
        firstName = requireText(value, "First name");
    }

    public void setSssNumber(String value) {
        sssNumber = requireText(value, "SSS number");
    }

    public void setPhilHealthNumber(String value) {
        philHealthNumber = requireText(value, "PhilHealth number");
    }

    public void setTinNumber(String value) {
        tinNumber = requireText(value, "TIN");
    }

    public void setPagIbigNumber(String value) {
        pagIbigNumber = requireText(value, "Pag-IBIG number");
    }

    public void setStatus(String value) {
        status = requireText(value, "Status");
    }

    public void setImmediateSupervisor(String value) {
        immediateSupervisor = requireText(value, "Supervisor");
    }

    public void setPosition(String value) {
        position = requireText(value, "Position");
    }

    public void setRiceSubsidy(double value) {
        riceSubsidy = requireNonnegative(value, "Rice subsidy");
    }

    public void setPhoneAllowance(double value) {
        phoneAllowance = requireNonnegative(value, "Phone allowance");
    }

    public void setClothingAllowance(double value) {
        clothingAllowance = requireNonnegative(value, "Clothing allowance");
    }

    public void setHourlyRate(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(
                    "Hourly rate must be greater than zero.");
        }
        hourlyRate = value;
    }

    public abstract double calculatePay();
}