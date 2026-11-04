package motorph;

import static motorph.TestSupport.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import motorph.deduction.PagIbigDeduction;
import motorph.deduction.PhilHealthDeduction;
import motorph.deduction.SSSDeduction;
import motorph.deduction.WithholdingTaxDeduction;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.Payslip;
import motorph.service.CSVDataHandler;
import motorph.service.PayrollProcessor;

/**
 * Runs the OOP backend for all 34 employees in all 14 cutoffs and compares every
 * row and every summary with the saved output of the original CP2 code
 * (test/cp2-expected, see the README there).
 * <p>
 * Three rows are expected to differ by 0.01. In each, the exact gross pay ends
 * in a half centavo, and CP2's double arithmetic stored it slightly below the
 * half, so CP2 rounded down. BigDecimal keeps the exact value and rounds up.
 * </p>
 */
public class Cp2ComparisonTest {

    private static final Path EXPECTED_ROWS    = Path.of("test/cp2-expected/cp2_payroll_results.csv");
    private static final Path EXPECTED_SUMMARY = Path.of("test/cp2-expected/cp2_payroll_summary.csv");

    /** cutoff|employeeId -> { CP2 value, correct value }. Exact amounts shown in the comment. */
    private static final Map<String, String[]> KNOWN_CP2_ROUNDING_ERRORS = Map.of(
        "June 1-15, 2024|10030",      new String[]{"10379.57", "10379.58"},   // 133.93 x 4,650 min / 60 = 10,379.575
        "July 1-15, 2024|10011",      new String[]{"25563.78", "25563.79"},   // 302.53 x 5,070 min / 60 = 25,563.785
        "December 16-31, 2024|10012", new String[]{"20058.33", "20058.34"}    // 229.02 x 5,255 min / 60 = 20,058.335
    );

    public static void main(String[] args) throws IOException {
        run();
        finish();
    }

    static void run() throws IOException {
        CSVDataHandler csv = new CSVDataHandler();
        PayrollProcessor processor = new PayrollProcessor(
                List.of(new SSSDeduction(), new PhilHealthDeduction(), new PagIbigDeduction()),
                new WithholdingTaxDeduction());
        List<Employee> employees = csv.loadEmployees();
        Map<String, List<AttendanceRecord>> attendance = csv.loadAttendance();

        Map<String, String> expectedRows = readExpected(EXPECTED_ROWS);
        Map<String, String> expectedSummary = readExpected(EXPECTED_SUMMARY);

        section("CP2 comparison: cutoff list");
        List<String> cutoffs = processor.getAvailableCutoffs(attendance);
        checkEquals(14, cutoffs.size(), "14 cutoffs found (June to December 2024)");
        checkEquals("June 1-15, 2024", cutoffs.get(0), "first cutoff in order");
        checkEquals("December 16-31, 2024", cutoffs.get(13), "last cutoff in order");

        int rows = 0;
        int exactMatches = 0;
        int knownDifferences = 0;
        int summaryMatches = 0;
        for (String cutoff : cutoffs) {
            List<Payslip> payslips = processor.processPayroll(employees, attendance, cutoff);
            for (Payslip payslip : payslips) {
                rows++;
                String key = cutoff + "|" + payslip.getEmployeeId();
                String cp2Row = expectedRows.get(key);
                String ourRow = payslip.toCSVRow();
                String[] knownError = KNOWN_CP2_ROUNDING_ERRORS.get(key);
                if (knownError == null) {
                    if (ourRow.equals(cp2Row)) {
                        exactMatches++;
                    } else {
                        check(false, "row matches CP2: " + key + "\n      CP2: " + cp2Row + "\n      OOP: " + ourRow);
                    }
                } else {
                    String corrected = cp2Row.replace(knownError[0], knownError[1]);
                    check(!ourRow.equals(cp2Row) && ourRow.equals(corrected),
                            "known CP2 rounding error " + key + ": CP2 " + knownError[0] + " -> OOP " + knownError[1]);
                    knownDifferences++;
                }
            }

            Map<String, BigDecimal> summary = processor.computePayrollSummary(payslips);
            String ourSummary = String.format(Locale.US, "%d,%.2f,%.2f,%.2f,%.2f",
                    summary.get(PayrollProcessor.SUMMARY_TOTAL_EMPLOYEES).intValue(),
                    summary.get(PayrollProcessor.SUMMARY_TOTAL_GROSS),
                    summary.get(PayrollProcessor.SUMMARY_TOTAL_DEDUCTIONS),
                    summary.get(PayrollProcessor.SUMMARY_TOTAL_NET),
                    summary.get(PayrollProcessor.SUMMARY_AVERAGE_NET));
            if (ourSummary.equals(expectedSummary.get(cutoff))) {
                summaryMatches++;
            } else {
                check(false, "summary matches CP2 for " + cutoff + "\n      CP2: " + expectedSummary.get(cutoff)
                        + "\n      OOP: " + ourSummary);
            }
        }

        section("CP2 comparison: results");
        checkEquals(476, rows, "rows compared (34 employees x 14 cutoffs)");
        checkEquals(473, exactMatches, "rows identical to CP2 in every column");
        checkEquals(3, knownDifferences, "rows differing only by CP2's own half-centavo rounding error");
        checkEquals(14, summaryMatches, "cutoff summaries identical to CP2");
        System.out.println("\nSummary: " + exactMatches + " of " + rows + " rows identical to CP2; "
                + knownDifferences + " differ by 0.01 where CP2's double arithmetic rounded a half centavo down; "
                + summaryMatches + " of 14 summaries identical.");
    }

    /** Reads a CP2 reference file into "cutoff|employeeId" (or "cutoff") -> rest of the line. */
    private static Map<String, String> readExpected(Path file) throws IOException {
        Map<String, String> expected = new HashMap<>();
        List<String> lines = Files.readAllLines(file);
        for (String line : lines.subList(1, lines.size())) {
            int end = line.indexOf("\",");
            String cutoff = line.substring(1, end);
            String rest = line.substring(end + 2);
            boolean isRow = file.equals(EXPECTED_ROWS);
            String key = isRow ? cutoff + "|" + rest.substring(0, rest.indexOf(',')) : cutoff;
            expected.put(key, rest);
        }
        return expected;
    }
}
