package Backend;

public class ThirteenthMonthPayrollProcessor extends PayrollProcessor {

    public ThirteenthMonthPayrollProcessor(Employee employee) {
        super(employee);
    }

   @Override
protected double computePay() {
    // 13th month pay = one month's basic salary (annual basic salary ÷ 12
   
    return employee.getBasicSalary();
}
    @Override
protected boolean appliesStatutoryDeductions() {
    return false;
}

@Override
protected boolean appliesAllowances() {
    return false;
}

    public static void main(String[] args) {
        Employee emp = PayrollDataLoader.loadEmployee("10001");
        ThirteenthMonthPayrollProcessor processor = new ThirteenthMonthPayrollProcessor(emp);

        Payslip payslip = processor.processPayroll("10001", "2024");
        System.out.println(payslip);
    }
}