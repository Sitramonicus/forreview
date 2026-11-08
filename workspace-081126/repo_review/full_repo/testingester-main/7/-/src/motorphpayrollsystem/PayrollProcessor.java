package motorphpayrollsystem;

public class PayrollProcessor {

    private final PayrollCalculator payrollCalculator;
    private final ContributionCalculator contributionCalculator;

    private AttendanceRecord attendanceRecord;
    private OvertimeRequest overtimeRequest;

    public PayrollProcessor() {
        this.payrollCalculator = new PayrollCalculator();
        this.contributionCalculator = new ContributionCalculator();
    }

    public void setAttendanceRecord(AttendanceRecord attendanceRecord) {
        this.attendanceRecord = attendanceRecord;
    }

    public void setOvertimeRequest(OvertimeRequest overtimeRequest) {
        this.overtimeRequest = overtimeRequest;
    }

    public Payslip processPayroll(Employee employee, String payPeriod) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null.");
        }
        if (payPeriod == null || payPeriod.trim().isEmpty()) {
            throw new IllegalArgumentException("Pay period cannot be empty.");
        }

        double regularPay;
        if (attendanceRecord != null) {
            regularPay = attendanceRecord.calculateHoursWorked()
                    * employee.getHourlyRate();
        } else {
            regularPay = payrollCalculator.calculateRegularPay(employee);
        }

        // CP2's original hours-based calculation already includes all worked hours.
        double overtimePay = 0.0;
        double grossPay = payrollCalculator.calculateGrossPay(
                regularPay, overtimePay, employee);

        double semiMonthlySalary;
        if (employee instanceof FullTimeEmployee) {
            semiMonthlySalary = ((FullTimeEmployee) employee).getBasicSalary() / 2.0;
        } else {
            semiMonthlySalary = payrollCalculator.calculateRegularPay(employee);
        }

        double sss = contributionCalculator.calculateSSS(semiMonthlySalary);
        double philHealth = contributionCalculator.calculatePhilHealth(semiMonthlySalary);
        double pagIBIG = contributionCalculator.calculatePagIBIG(semiMonthlySalary);
        double contributions = contributionCalculator.calculateTotalContributions(
                sss, philHealth, pagIBIG);
        double taxableIncome = Math.max(0.0, grossPay - contributions);
        double withholdingTax = contributionCalculator.calculateWithholdingTax(taxableIncome);
        double totalDeductions = contributions + withholdingTax;

        return new Payslip(employee.getEmployeeID(), payPeriod,
                grossPay, totalDeductions);
    }

    /**
     * GUI integration adapter: uses the OOP calculators but returns the
     * existing detailed result expected by PayrollAdminUI.
     * Preserves CP2's hours-based gross pay and semi-monthly deduction basis.
     */
    public Payroll.PayrollResult processPayrollForGUI(String[] emp, double hoursWorked) {
        if (emp == null || emp.length < 19) {
            throw new IllegalArgumentException("Employee CSV data is incomplete.");
        }
        if (!Double.isFinite(hoursWorked) || hoursWorked < 0) {
            throw new IllegalArgumentException("Hours worked must be valid and nonnegative.");
        }

        FullTimeEmployee employee = EmployeeMapper.fromCSV(emp);
        double basicSalary = Payroll.parseAmount(emp[13]);
        double hourlyRate = Payroll.parseAmount(emp[18]);
        if (hourlyRate <= 0 && basicSalary > 0) {
            hourlyRate = Payroll.basicToHourly(basicSalary);
        }

        double regularPay = hoursWorked * hourlyRate;
        double grossPay = payrollCalculator.calculateGrossPay(
                regularPay, 0.0, employee);

        double semiMonthlySalary = Payroll.parseAmount(emp[17]);
        if (semiMonthlySalary <= 0 && basicSalary > 0) {
            semiMonthlySalary = Payroll.basicToSemiMonthly(basicSalary);
        }

        double sss = contributionCalculator.calculateSSS(semiMonthlySalary);
        double philHealth = contributionCalculator.calculatePhilHealth(semiMonthlySalary);
        double pagIbig = contributionCalculator.calculatePagIBIG(semiMonthlySalary);
        double contributions = contributionCalculator.calculateTotalContributions(
                sss, philHealth, pagIbig);
        double taxableIncome = Math.max(0.0, grossPay - contributions);
        double withholdingTax = contributionCalculator.calculateWithholdingTax(taxableIncome);
        double totalDeductions = contributions + withholdingTax;

        Payroll.PayrollResult result = new Payroll.PayrollResult();
        result.empNo = emp[0];
        result.lastName = emp[1];
        result.firstName = emp[2];
        result.position = emp[11];
        result.basicSalary = basicSalary;
        result.hourlyRate = hourlyRate;
        result.hoursWorked = hoursWorked;
        result.riceSubsidy = employee.getRiceSubsidy();
        result.phoneAllow = employee.getPhoneAllowance();
        result.clothingAllow = employee.getClothingAllowance();
        result.totalAllowances = result.riceSubsidy + result.phoneAllow
                + result.clothingAllow;
        result.grossPay = grossPay;
        result.sssDeduction = sss;
        result.philHealth = philHealth;
        result.pagIbig = pagIbig;
        result.withholdingTax = withholdingTax;
        result.totalDeductions = totalDeductions;
        result.netPay = Math.max(0.0, grossPay - totalDeductions);
        return result;
    }
}