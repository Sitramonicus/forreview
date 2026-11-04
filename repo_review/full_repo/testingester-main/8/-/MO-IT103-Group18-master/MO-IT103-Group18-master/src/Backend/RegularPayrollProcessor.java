package Backend;

import java.time.Month;

public class RegularPayrollProcessor extends PayrollProcessor {

    private final Month month;
    private final int year;

    public RegularPayrollProcessor(Employee employee, Month month, int year) {
        super(employee);
        this.month = month;
        this.year = year;
    }

    @Override
    protected double computePay() {
        // Total hours worked this month, break already deducted
        // (no overtime premium applied here — overtime only gets
        // added once we have an actual approved OvertimeRecord for
        // this employee/month, which we don't yet have a data source for)
        double totalHours = PayrollDataLoader.getTotalHoursWorked(
                employee.getEmployeeId(), month, year);

        //  allowances are excluded on purpose — they go on the
        // payslip separately, not into taxable gross pay
        return employee.calculateSalary(totalHours);
    }

    public static void main(String[] args) {
    Employee emp = PayrollDataLoader.loadEmployee("10001");
    RegularPayrollProcessor processor = new RegularPayrollProcessor(emp, Month.JUNE, 2024);

    Payslip payslip = processor.processPayroll("10001", "June 2024");
    System.out.println(payslip);
}

}