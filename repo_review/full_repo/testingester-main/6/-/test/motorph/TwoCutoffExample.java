package motorph;

import static motorph.TestSupport.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.Payslip;
import motorph.service.CSVDataHandler;
import motorph.service.PayrollProcessor;

/**
 * Worked two-cutoff example (MS1 feedback): one Regular and one Probationary
 * employee, June 2024, run through the same Employee references. Prints
 * itemized payslips for both cutoffs and checks that monthly deductions and
 * allowances are applied once, in the 2nd cutoff, without duplication.
 */
public class TwoCutoffExample {

    private static final String FIRST_CUTOFF  = "June 1-15, 2024";
    private static final String SECOND_CUTOFF = "June 16-30, 2024";

    public static void main(String[] args) throws IOException {
        run();
        finish();
    }

    static void run() throws IOException {
        CSVDataHandler csv = new CSVDataHandler();
        PayrollProcessor processor = PayrollProcessorTest.standardProcessor();
        Map<String, List<AttendanceRecord>> attendance = csv.loadAttendance();
        List<Employee> employees = List.of(csv.findEmployeeById("10002"), csv.findEmployeeById("10021"));

        for (Employee employee : employees) {
            List<AttendanceRecord> records = attendance.get(employee.getEmployeeId());
            Payslip first = processor.generatePayslip(employee, records, FIRST_CUTOFF);
            Payslip second = processor.generatePayslip(employee, records, SECOND_CUTOFF);

            section("Two-cutoff example: " + employee.getEmployeeId() + " " + employee.getFullName()
                    + " (" + employee.getEmploymentType() + "), hourly rate " + money(employee.getHourlyRate()));
            printPayslip(first, null, processor);
            BigDecimal monthlyGross = employee.computeGrossPay(first.getHoursWorked() + second.getHoursWorked());
            printPayslip(second, monthlyGross, processor);

            System.out.println("  Month check");
            System.out.printf("    Deductions taken: %s (1st) + %s (2nd) = %s%n", money(first.getTotalDeductions()),
                    money(second.getTotalDeductions()), money(first.getTotalDeductions().add(second.getTotalDeductions())));
            System.out.printf("    Net pay for the month: %s + %s = %s%n", money(first.getNetPay()),
                    money(second.getNetPay()), money(first.getNetPay().add(second.getNetPay())));

            checkAmount("0.00", first.getTotalDeductions(), "1st cutoff has no deductions");
            checkEquals(first.getGrossPay(), first.getNetPay(), "1st cutoff net pay = gross pay");
            checkEquals(processor.computeTotalDeductions(monthlyGross),
                    first.getTotalDeductions().add(second.getTotalDeductions()),
                    "month's deductions taken exactly once (= computeTotalDeductions(monthly gross))");
            checkAmount(money(monthlyGross.subtract(processor.computeTotalDeductions(monthlyGross))).replace(",", ""),
                    first.getNetPay().add(second.getNetPay()),
                    "net 1st + net 2nd = monthly gross - monthly deductions");
            checkEquals(employee.computeAllowances(), first.getAllowances().add(second.getAllowances()),
                    "allowances listed once for the month (" + employee.getEmploymentType() + " version runs)");
        }
    }

    private static void printPayslip(Payslip payslip, BigDecimal monthlyGross, PayrollProcessor processor) {
        System.out.println("  " + payslip.getCutoffLabel());
        line("Hours worked", String.format("%.2f", payslip.getHoursWorked()), "");
        line("Gross pay", money(payslip.getGrossPay()), "hours x hourly rate");
        if (monthlyGross != null) {
            line("Monthly gross", money(monthlyGross), "1st + 2nd cutoff gross: base for all deductions");
        }
        BigDecimal taxable = monthlyGross;
        List<String> names = processor.getDeductionNames();
        Map<String, BigDecimal> breakdown = payslip.getDeductionBreakdown();
        for (int i = 0; i < names.size(); i++) {
            boolean isTax = i == names.size() - 1;
            if (isTax && taxable != null) {
                line("Taxable income", money(taxable), "monthly gross - SSS - PhilHealth - Pag-IBIG");
            }
            line(names.get(i), money(breakdown.get(names.get(i))), monthlyGross == null ? "1st cutoff: none" : "");
            if (!isTax && taxable != null) {
                taxable = taxable.subtract(breakdown.get(names.get(i)));
            }
        }
        line("Total deductions", money(payslip.getTotalDeductions()), "");
        line("Net pay", money(payslip.getNetPay()), monthlyGross == null ? "= gross pay" : "cutoff gross - total deductions");
        line("Allowances", money(payslip.getAllowances()), monthlyGross == null
                ? "monthly item: listed on the 2nd cutoff" : "listed only, not added to net pay (CP2)");
        System.out.println();
    }

    private static void line(String label, String amount, String note) {
        System.out.printf("    %-18s %14s   %s%n", label, amount, note);
    }

    private static String money(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}
