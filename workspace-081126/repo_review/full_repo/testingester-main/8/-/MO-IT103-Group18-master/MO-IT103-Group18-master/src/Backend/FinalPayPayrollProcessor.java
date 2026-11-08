package Backend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Final pay for a separated employee (resigned, end of contract, terminated).
// Follows DOLE Labor Advisory No. 06-2020, which lists the components of final pay:
//   1. Unpaid last salary   = hours worked from the 1st of the separation month
//                             up to the separation date x hourly rate
//   2. Prorated 13th month  = basic salary x months worked this year / 12
//   3. Leave conversion     = unused leave credits x daily rate (hourly rate x 8)
//
// Deductions (overridden from PayrollProcessor):
//   - SSS, PhilHealth, Pag-IBIG apply to the last salary only.
//   - Withholding tax applies to the last salary (after contributions) plus any amount
//     of 13th month + leave conversion above the P90,000 tax-exempt ceiling (TRAIN law).
public class FinalPayPayrollProcessor extends PayrollProcessor {

    private static final int LEAVE_CREDITS_PER_YEAR = 15;          // MotorPH leave policy (group assumption)
    private static final double TAX_EXEMPT_BENEFITS_CEILING = 90000; // 13th month + other benefits (TRAIN law)
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final LocalDate separationDate;
    private final List<LeaveRecord> leavesTaken;

    // Components, filled in by computePay() so the payslip can show the breakdown
    private double lastSalary;
    private double proratedThirteenthMonth;
    private double leaveConversion;
    private double unusedLeaveDays;

    public FinalPayPayrollProcessor(Employee employee, LocalDate separationDate, List<LeaveRecord> leavesTaken) {
        super(employee);
        if (separationDate == null) {
            throw new IllegalArgumentException("Separation date is required for final pay.");
        }
        this.separationDate = separationDate;
        this.leavesTaken = (leavesTaken == null) ? new ArrayList<>() : leavesTaken;
    }

    // Use when the employee took no leave this year (MotorPH has no leave CSV yet)
    public FinalPayPayrollProcessor(Employee employee, LocalDate separationDate) {
        this(employee, separationDate, new ArrayList<>());
    }

    @Override
    public double computePay() {
        lastSalary = computeLastSalary();
        proratedThirteenthMonth = computeProratedThirteenthMonth();
        leaveConversion = computeLeaveConversion();
        return lastSalary + proratedThirteenthMonth + leaveConversion;
    }

    // 1. Hours worked from the 1st of the separation month up to the separation date
    private double computeLastSalary() {
        double hours = 0;
        for (AttendanceRecord record : PayrollDataLoader.loadAttendance(
                employee.getEmployeeId(), separationDate.getMonth(), separationDate.getYear())) {
            LocalDate workDate = LocalDate.parse(record.getDate(), DATE_FORMAT);
            if (!workDate.isAfter(separationDate)) {
                hours += record.calculateHoursWorked();
            }
        }
        return employee.calculateSalary(hours);
    }

    // 2. Months worked this year: full months before the separation month,
    //    plus the fraction of the separation month worked (e.g., June 15 = 5.5 months).
    //    Assumes the employee was employed since January 1 (the CSV has no hire date).
    private double computeProratedThirteenthMonth() {
        double monthsWorked = (separationDate.getMonthValue() - 1)
                + (double) separationDate.getDayOfMonth() / separationDate.lengthOfMonth();
        return employee.getBasicSalary() * monthsWorked / 12.0;
    }

    // 3. Unused leave credits = yearly credits - approved PAID leave days used this year
    private double computeLeaveConversion() {
        int daysUsed = 0;
        for (LeaveRecord leave : leavesTaken) {
            boolean sameEmployee = leave.getEmployeeId().equals(employee.getEmployeeId());
            boolean paid = "Paid".equalsIgnoreCase(leave.getPaidStatus());
            int leaveYear = LocalDate.parse(leave.getStartDate(), DATE_FORMAT).getYear();
            if (sameEmployee && paid && leaveYear == separationDate.getYear()) {
                daysUsed += leave.getDuration(); // 0 unless Approved
            }
        }
        unusedLeaveDays = Math.max(LEAVE_CREDITS_PER_YEAR - daysUsed, 0);
        double dailyRate = employee.getHourlyRate() * 8;
        return unusedLeaveDays * dailyRate;
    }

    // Contributions apply only to the salary actually earned, not to benefits
    @Override
    protected double getContributionBase(double grossPay) {
        return lastSalary;
    }

    // Taxable = last salary after contributions + benefits above the P90,000 ceiling
    @Override
    protected double getTaxableIncome(double grossPay, double contributions) {
        double benefits = proratedThirteenthMonth + leaveConversion;
        double taxableBenefits = Math.max(benefits - TAX_EXEMPT_BENEFITS_CEILING, 0);
        return (lastSalary - contributions) + taxableBenefits;
    }

    // Allowances are paid with the regular payroll, not again in final pay
    @Override
    protected boolean appliesAllowances() {
        return false;
    }

    @Override
    public String getGrossPayBreakdown() {
        return String.format(
                "Separation Date: %s%n"
              + "Last Salary (up to separation date): %,.2f%n"
              + "Prorated 13th Month Pay: %,.2f%n"
              + "Leave Conversion (%.0f unused days): %,.2f",
                separationDate.format(DATE_FORMAT), lastSalary, proratedThirteenthMonth,
                unusedLeaveDays, leaveConversion);
    }

    public LocalDate getSeparationDate() { return separationDate; }
    public double getLastSalary() { return lastSalary; }
    public double getProratedThirteenthMonth() { return proratedThirteenthMonth; }
    public double getLeaveConversion() { return leaveConversion; }
    public double getUnusedLeaveDays() { return unusedLeaveDays; }

    public static void main(String[] args) {
        Employee emp = PayrollDataLoader.loadEmployee("10001");

        // Example 1: separated at the end of June 2024, no leave taken
        FinalPayPayrollProcessor endOfMonth = new FinalPayPayrollProcessor(emp, LocalDate.of(2024, 6, 30));
        Payslip p1 = endOfMonth.processPayroll("10001", "Final Pay (06-30-2024)");
        System.out.println(endOfMonth.getGrossPayBreakdown());
        System.out.println(p1);
        System.out.println();

        // Example 2: separated mid-month, after 3 approved paid leave days
        List<LeaveRecord> leaves = new ArrayList<>();
        leaves.add(new LeaveRecord("10001", "Vacation", "03-11-2024", "03-13-2024", "Approved", "Paid"));
        FinalPayPayrollProcessor midMonth = new FinalPayPayrollProcessor(emp, LocalDate.of(2024, 6, 14), leaves);
        Payslip p2 = midMonth.processPayroll("10001", "Final Pay (06-14-2024)");
        System.out.println(midMonth.getGrossPayBreakdown());
        System.out.println(p2);
    }
}