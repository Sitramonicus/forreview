package motorph;

import static motorph.TestSupport.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import motorph.deduction.Deduction;
import motorph.deduction.PagIbigDeduction;
import motorph.deduction.PhilHealthDeduction;
import motorph.deduction.SSSDeduction;
import motorph.deduction.WithholdingTaxDeduction;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.Payslip;
import motorph.service.CSVDataHandler;
import motorph.service.PayrollProcessor;

/** Tests the PayrollProcessor workflow, Payslip immutability, and saving results. */
public class PayrollProcessorTest {

    public static void main(String[] args) throws IOException {
        run();
        finish();
    }

    static PayrollProcessor standardProcessor() {
        return new PayrollProcessor(
                List.of(new SSSDeduction(), new PhilHealthDeduction(), new PagIbigDeduction()),
                new WithholdingTaxDeduction());
    }

    private static AttendanceRecord day(String date, String in, String out) {
        return new AttendanceRecord("10001", LocalDate.parse(date), LocalTime.parse(in), LocalTime.parse(out));
    }

    static void run() throws IOException {
        PayrollProcessor processor = standardProcessor();

        section("PayrollProcessor: construction (deductions passed into the constructor)");
        checkRejects(() -> new PayrollProcessor(null, new WithholdingTaxDeduction()), "contribution list required");
        checkRejects(() -> new PayrollProcessor(List.of(new SSSDeduction()), null), "withholding tax required");
        checkEquals(List.of("SSS", "PhilHealth", "Pag-IBIG", "Withholding Tax"), processor.getDeductionNames(),
                "deduction order: contributions, then tax");

        section("PayrollProcessor: hours, deductions, net pay");
        List<AttendanceRecord> records = List.of(
                day("2024-06-03", "08:05", "17:30"),   // grace + cap: 9 h, 1st cutoff
                day("2024-06-04", "09:00", "17:00"),   // 8 h, 1st cutoff
                day("2024-06-17", "08:00", "12:00"));  // 4 h, 2nd cutoff
        checkEquals(17.0, processor.computeHoursForCutoff(records, "June 1-15, 2024"), "1st cutoff hours = 9 + 8");
        checkEquals(4.0, processor.computeHoursForCutoff(records, "June 16-30, 2024"), "2nd cutoff hours = 4");
        checkEquals(0.0, processor.computeHoursForCutoff(records, "July 1-15, 2024"), "no matching records gives 0");
        checkEquals(0.0, processor.computeHoursForCutoff(null, "June 1-15, 2024"), "no records at all gives 0");

        BigDecimal monthlyGross = new BigDecimal("50000");
        Map<String, BigDecimal> breakdown = processor.computeDeductionBreakdown(monthlyGross);
        checkAmount("1125.00", breakdown.get("SSS"), "SSS on 50,000");
        checkAmount("750.00", breakdown.get("PhilHealth"), "PhilHealth on 50,000");
        checkAmount("100.00", breakdown.get("Pag-IBIG"), "Pag-IBIG on 50,000");
        checkAmount("6173.00", breakdown.get("Withholding Tax"), "tax on taxable 48,025, not on gross 50,000");
        checkAmount("8148.00", processor.computeTotalDeductions(monthlyGross), "total = sum of the breakdown");
        checkAmount("285.00", processor.computeTotalDeductions(BigDecimal.ZERO),
                "0 gross month: SSS 135 + PhilHealth min 150 + Pag-IBIG 0 + tax 0 (taxable floored at 0)");
        checkAmount("0.00", processor.computeNetPay(new BigDecimal("100"), new BigDecimal("285")), "net pay never below 0");

        section("PayrollProcessor: generatePayslip() cutoff rule");
        CSVDataHandler csv = new CSVDataHandler();
        Employee regular = csv.findEmployeeById("10001");
        Payslip first = processor.generatePayslip(regular, records, "June 1-15, 2024");
        checkAmount("0.00", first.getTotalDeductions(), "1st cutoff: no deductions");
        checkEquals(first.getGrossPay(), first.getNetPay(), "1st cutoff: net pay = gross pay");
        checkAmount("0.00", first.getAllowances(), "1st cutoff: monthly allowances not listed yet");
        Payslip second = processor.generatePayslip(regular, records, "June 16-30, 2024");
        BigDecimal combinedGross = regular.computeGrossPay(17 + 4);
        checkAmount("11249.91", combinedGross, "monthly gross = (17 + 4) h x 535.71");
        checkEquals(processor.computeTotalDeductions(combinedGross), second.getTotalDeductions(),
                "2nd cutoff deductions use the combined monthly gross, not the 2nd cutoff gross alone");
        checkAmount("4500.00", second.getAllowances(), "2nd cutoff lists the month's allowances once");
        checkRejects(() -> processor.generatePayslip(null, records, "June 1-15, 2024"), "employee required");

        section("Payslip: immutable");
        BigDecimal sssBefore = second.getDeductionAmount("SSS");
        Map<String, BigDecimal> copy = second.getDeductionBreakdown();
        copy.put("SSS", new BigDecimal("999999"));
        checkEquals(sssBefore, second.getDeductionAmount("SSS"), "editing the returned breakdown does not change the payslip");
        checkEquals(second.getTotalDeductions(),
                second.getDeductionBreakdown().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                "payslip total equals the sum of its itemized deductions");

        section("PayrollProcessor: all employees, attendance warnings, summary");
        List<Employee> employees = csv.loadEmployees();
        Map<String, List<AttendanceRecord>> attendance = csv.loadAttendance();
        List<Payslip> payslips = processor.processPayroll(employees, attendance, "June 16-30, 2024");
        checkEquals(34, payslips.size(), "one payslip per employee");
        checkEquals(0, processor.findEmployeesWithoutAttendance(employees, attendance, "June 16-30, 2024").size(),
                "every employee has June 16-30 attendance");
        checkEquals(34, processor.findEmployeesWithoutAttendance(employees, attendance, "January 1-15, 2025").size(),
                "a cutoff with no attendance lists all 34 employees");
        Map<String, BigDecimal> summary = processor.computePayrollSummary(payslips);
        checkEquals(34, summary.get(PayrollProcessor.SUMMARY_TOTAL_EMPLOYEES).intValue(), "summary counts 34 employees");
        checkAmount("560704.82", summary.get(PayrollProcessor.SUMMARY_TOTAL_GROSS), "summary total gross matches CP2");
        checkRejects(() -> processor.computePayrollSummary(List.of()), "summary of an empty list rejected");

        section("Polymorphism: a new Deduction subclass needs no PayrollProcessor change");
        Deduction flatFee = new Deduction("Test Fee") {
            @Override
            public BigDecimal computeDeduction(BigDecimal baseAmount) {
                validateBaseAmount(baseAmount);
                return BigDecimal.TEN;
            }
        };
        PayrollProcessor extended = new PayrollProcessor(
                List.of(new SSSDeduction(), new PhilHealthDeduction(), new PagIbigDeduction(), flatFee),
                new WithholdingTaxDeduction());
        checkAmount("8155.50", extended.computeTotalDeductions(monthlyGross),
                "extra 10 fee: 8,148 + 10 - 2.50 (taxable is 10 lower, so tax drops 25% of 10)");

        section("CSVDataHandler: savePayrollResults() (temporary file)");
        Path results = Files.createTempFile("payroll_results", ".csv");
        CSVDataHandler tempCsv = new CSVDataHandler(Path.of(CSVDataHandler.DEFAULT_EMPLOYEE_CSV),
                Path.of(CSVDataHandler.DEFAULT_ATTENDANCE_CSV), results);
        tempCsv.savePayrollResults(payslips);
        List<String> lines = Files.readAllLines(results);
        checkEquals("Employee ID,Name,Hourly Rate,Hours Worked,Gross Pay,SSS,PhilHealth,Pag-IBIG,"
                + "Withholding Tax,Total Deductions,Net Pay", lines.get(0), "header matches CP2's column order");
        checkEquals(35, lines.size(), "header + 34 rows");
        checkEquals("10001,Manuel III Garcia,535.71,76.82,41151.46,1125.00,900.00,100.00,14567.53,16692.53,24458.93",
                lines.get(1), "first row matches CP2's saved format and values");
        checkRejects(() -> {
            try {
                tempCsv.savePayrollResults(List.of());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }, "saving an empty result list rejected");
    }
}
