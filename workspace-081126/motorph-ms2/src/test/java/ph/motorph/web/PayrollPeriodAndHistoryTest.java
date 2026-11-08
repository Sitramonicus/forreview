package ph.motorph.web;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payroll cycles and effective-dated rates.
 *
 * <p>Two claims are being pinned here:
 *
 * <ol>
 *   <li>A payroll period is one set of dates shared by everybody, and it is validated against its
 *       cadence — so a semi-monthly second cutoff ends on the 28th, 29th, 30th or 31st of the
 *       month, never on a typed-in "30".</li>
 *   <li>A rate change does not rewrite an earlier payslip. The rate in force at the end of the
 *       period is the one that pays.</li>
 * </ol>
 */
public final class PayrollPeriodAndHistoryTest {

    private int checks;

    public static void main(String[] args) {
        new PayrollPeriodAndHistoryTest().run();
    }

    private void run() {
        periodAcceptsTheDocumentedCadences();
        periodRejectsWrongDateRanges();
        periodIsSharedAndClosesOnce();
        salaryHistoryPaysTheRateInForce();
        salaryHistoryRefusesAmbiguousRows();
        historicalPayslipIsUnchangedByALaterRaise();
        System.out.println("PASS: " + checks + " payroll period and salary history checks");
    }

    private void periodAcceptsTheDocumentedCadences() {
        PayrollPeriod monthly = PayrollPeriod.open(PayrollPeriod.Cadence.MONTHLY,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "2026-01-01T00:00:00Z");
        equal("PP-M-2026-01-01-2026-01-31", monthly.id(), "monthly period id carries both dates");
        equal("MONTHLY", monthly.payPeriod(), "monthly cadence is carried on the period");
        equal(PayrollPeriod.Status.OPEN, monthly.status(), "a new period is open");

        PayrollPeriod firstHalf = PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), "x");
        check(firstHalf.acceptsRuns(), "the 1st-15th is a valid first cutoff");

        // February: the second cutoff ends on the 28th, not a hard-coded 30th.
        PayrollPeriod feb = PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 28), "x");
        check(feb.acceptsRuns(), "16th to the 28th of February is a valid second cutoff");
        PayrollPeriod leap = PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2028, 2, 16), LocalDate.of(2028, 2, 29), "x");
        check(leap.acceptsRuns(), "16th to the 29th is valid in a leap year");
        PayrollPeriod april = PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 4, 16), LocalDate.of(2026, 4, 30), "x");
        check(april.acceptsRuns(), "16th to the 30th is valid for a 30-day month");
    }

    private void periodRejectsWrongDateRanges() {
        // The old form let any dates be typed. These are the cases that would have slipped through.
        expectRejected(() -> PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "x"), "1st-31st is monthly, not semi-monthly");
        expectRejected(() -> PayrollPeriod.open(PayrollPeriod.Cadence.MONTHLY,
            LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 31), "x"), "monthly must start on the 1st");
        expectRejected(() -> PayrollPeriod.open(PayrollPeriod.Cadence.MONTHLY,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 30), "x"), "monthly must end on the last day");
        expectRejected(() -> PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20), "x"), "a ten-day range is not a cutoff");
        expectRejected(() -> PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 1, 16), LocalDate.of(2026, 2, 15), "x"), "a cutoff cannot span two months");
        expectRejected(() -> PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 1), "x"), "end before start is refused");
    }

    private void periodIsSharedAndClosesOnce() {
        // The document's own sample payslip runs 1/2/2026-1/15/2026, which sits inside the 1st-15th
        // cutoff; a period is the cycle, so it starts on the 1st even when the payslip starts later.
        PayrollPeriod jan = PayrollPeriod.open(PayrollPeriod.Cadence.SEMI_MONTHLY,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), "2026-01-01T00:00:00Z");
        equal(LocalDate.of(2026, 1, 1), jan.start(),
            "the cycle starts on the 1st even when the sample payslip starts on the 2nd");

        PayrollPeriod closed = jan.withStatus(PayrollPeriod.Status.CLOSED, "2026-01-16T00:00:00Z");
        equal(PayrollPeriod.Status.CLOSED, closed.status(), "closing sets the status");
        check(!closed.acceptsRuns(), "a closed period accepts no further runs");
        check("2026-01-16T00:00:00Z".equals(closed.closedAt()), "the close is timestamped");
        check(jan.acceptsRuns(), "closing returns a new period and leaves the original open");
    }

    private void salaryHistoryPaysTheRateInForce() {
        SalaryHistory history = new SalaryHistory();
        history.add(rate("10015", "2026-01-01", "53500.00", "1500.00", "1000.00", "1000.00"));
        history.add(rate("10015", "2026-07-01", "58000.00", "1500.00", "2000.00", "1000.00"));

        check(history.rateOn("10015", LocalDate.of(2026, 1, 15)).get().monthlyBasic()
            .compareTo(new BigDecimal("53500.00")) == 0, "January pays the January rate");
        check(history.rateOn("10015", LocalDate.of(2026, 6, 30)).get().monthlyBasic()
            .compareTo(new BigDecimal("53500.00")) == 0, "the day before a raise is still the old rate");
        check(history.rateOn("10015", LocalDate.of(2026, 7, 1)).get().monthlyBasic()
            .compareTo(new BigDecimal("58000.00")) == 0, "the raise takes effect on its own date");
        check(history.rateOn("10015", LocalDate.of(2026, 12, 31)).get().phoneAllowance()
            .compareTo(new BigDecimal("2000.00")) == 0, "later periods pick up the new allowance");
        check(!history.rateOn("10015", LocalDate.of(2025, 12, 31)).isPresent(),
            "no rate before the first effective date is reported, not invented");
        equal(2, history.historyOf("10015").size(), "both rate rows are kept");
        check(history.hasHistory("10015"), "history is reported as present");
        check(!history.hasHistory("10001"), "an employee with no dated rows reports none");
    }

    private void salaryHistoryRefusesAmbiguousRows() {
        SalaryHistory history = new SalaryHistory();
        history.add(rate("10015", "2026-01-01", "53500.00", "1500.00", "1000.00", "1000.00"));
        expectRejected(() -> history.add(rate("10015", "2026-01-01", "60000.00", "1500.00", "1000.00", "1000.00")),
            "two rates for the same person and date are refused");
        expectRejected(() -> history.add(rate("1001", "2026-01-01", "1.00", "0.00", "0.00", "0.00")),
            "a rate needs a five-digit employee number");
        expectRejected(() -> new SalaryHistory.SalaryRate("10015", null, BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", ""), "a rate needs an effective date");
    }

    /** The whole point: editing today's salary must not move last month's payslip. */
    private void historicalPayslipIsUnchangedByALaterRaise() {
        SalaryHistory history = new SalaryHistory();
        // The CSV's opening rate for 10015.
        history.add(rate("10015", "2025-01-01", "53500.00", "1500.00", "1000.00", "1000.00"));
        Employee directoryRecord = new EmployeeService(new InMemoryEmployeeStore())
            .findEmployee("10015").orElseThrow(AssertionError::new);

        // Raise the person by editing the directory, exactly as the HR screen does.
        Employee raised = directoryRecord.withPay(new BigDecimal("60000.00"),
            new BigDecimal("1500.00"), new BigDecimal("2000.00"), new BigDecimal("1000.00"));

        // With dated history, January still pays 53,500 even though the record now says 60,000.
        Employee asPaidInJanuary = history.asOf(raised, LocalDate.of(2026, 1, 15));
        check(asPaidInJanuary.getMonthlyBasic().compareTo(new BigDecimal("53500.00")) == 0,
            "January pays the rate in force in January, not today's rate");
        check(asPaidInJanuary.getPhoneAllowance().compareTo(new BigDecimal("1000.00")) == 0,
            "January pays the January allowance");

        // Add the raise as a dated row and the same employee now pays 60,000 from July.
        history.add(rate("10015", "2026-07-01", "60000.00", "1500.00", "2000.00", "1000.00"));
        Employee asPaidInAugust = history.asOf(raised, LocalDate.of(2026, 8, 15));
        check(asPaidInAugust.getMonthlyBasic().compareTo(new BigDecimal("60000.00")) == 0,
            "August pays the raised rate");
        check(asPaidInAugust.getPhoneAllowance().compareTo(new BigDecimal("2000.00")) == 0,
            "August pays the raised allowance");

        // And an employee with no history is left exactly as the directory says.
        Employee noHistory = new EmployeeService(new InMemoryEmployeeStore())
            .findEmployee("10001").orElseThrow(AssertionError::new);
        check(history.asOf(noHistory, LocalDate.of(2026, 1, 15)) == noHistory,
            "with no dated rate the directory record is used unchanged");
    }

    private SalaryHistory.SalaryRate rate(String employeeId, String from, String basic, String rice,
                                          String phone, String clothing) {
        return new SalaryHistory.SalaryRate(employeeId, LocalDate.parse(from),
            new BigDecimal(basic), new BigDecimal(rice), new BigDecimal(phone), new BigDecimal(clothing),
            "test", "2026-01-01T00:00:00Z");
    }

    private void expectRejected(Runnable action, String message) {
        checks++;
        try {
            action.run();
            System.out.println("  FAIL " + message + ": expected a rejection but the input was accepted");
            failures++;
        } catch (IllegalArgumentException expected) {
            // Correct.
        }
    }

    private int failures;

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            System.out.println("  FAIL " + message);
            failures++;
        }
    }

    private void equal(Object expected, Object actual, String message) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("  FAIL " + message + ": expected " + expected + " but got " + actual);
            failures++;
        }
    }
}
