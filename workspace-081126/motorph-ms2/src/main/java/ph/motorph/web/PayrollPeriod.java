package ph.motorph.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * One payroll cycle that applies to every employee at once.
 *
 * <p>MotorPH's design document defines a payroll cycle by its start and end date
 * ({@code PayrollCycleStart} / {@code PayrollCycleEnd}) and a period on each payslip
 * ({@code periodStartDate} / {@code periodEndDate}). This class is the cycle: dates, cadence and
 * state belong to the cycle, never to an individual payslip, so two employees can never be paid
 * for two different ranges in the same run.
 *
 * <p>The dates are validated against the cadence, because "16th to the 30th" is wrong in February:
 *
 * <pre>
 * MONTHLY       the 1st to the last day of that month
 * SEMI_MONTHLY  the 1st to the 15th, or the 16th to the last day of the month
 * </pre>
 */
public final class PayrollPeriod {

    public enum Status {
        /** Being prepared. Payslips may be previewed; nothing is final. */
        OPEN,
        /** Runs have been computed and are being reviewed. */
        PROCESSING,
        /** Frozen: no further runs may be computed for this period. */
        CLOSED
    }

    public enum Cadence {
        MONTHLY, SEMI_MONTHLY
    }

    private final String id;
    private final Cadence cadence;
    private final LocalDate start;
    private final LocalDate end;
    private final Status status;
    private final String createdAt;
    private final String closedAt;

    public PayrollPeriod(String id, Cadence cadence, LocalDate start, LocalDate end,
                         Status status, String createdAt, String closedAt) {
        if (id == null || !id.matches("PP-[A-Z0-9-]{4,40}")) {
            throw new IllegalArgumentException("Payroll period ID is invalid.");
        }
        if (cadence == null) throw new IllegalArgumentException("A payroll period needs a cadence.");
        if (start == null || end == null) throw new IllegalArgumentException("A payroll period needs both dates.");
        if (end.isBefore(start)) throw new IllegalArgumentException("The period end cannot be before its start.");
        validateCadence(cadence, start, end);
        this.id = id;
        this.cadence = cadence;
        this.start = start;
        this.end = end;
        this.status = status == null ? Status.OPEN : status;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
    }

    /** Creates a new open period, with an ID derived from the dates so duplicates are obvious. */
    public static PayrollPeriod open(Cadence cadence, LocalDate start, LocalDate end, String createdAt) {
        return new PayrollPeriod(idFor(cadence, start, end), cadence, start, end, Status.OPEN, createdAt, null);
    }

    public static String idFor(Cadence cadence, LocalDate start, LocalDate end) {
        return "PP-" + (cadence == Cadence.MONTHLY ? "M" : "S") + "-" + start + "-" + end;
    }

    /**
     * Rejects a date range that does not match the cadence. This is the guard the design document
     * implies and the old per-payslip form could not express: a semi-monthly cycle is the 1st–15th
     * or the 16th–end of month, and the end of month is computed, never typed as "30".
     */
    static void validateCadence(Cadence cadence, LocalDate start, LocalDate end) {
        LocalDate lastOfStartMonth = start.withDayOfMonth(start.lengthOfMonth());
        if (cadence == Cadence.MONTHLY) {
            if (start.getDayOfMonth() != 1 || !end.equals(lastOfStartMonth)) {
                throw new IllegalArgumentException(
                    "A monthly payroll period must run from the 1st to the last day of one month.");
            }
            return;
        }
        boolean firstHalf = start.getDayOfMonth() == 1 && end.getDayOfMonth() == 15
            && end.getMonth() == start.getMonth() && end.getYear() == start.getYear();
        boolean secondHalf = start.getDayOfMonth() == 16 && end.equals(lastOfStartMonth);
        if (!firstHalf && !secondHalf) {
            throw new IllegalArgumentException("A semi-monthly payroll period must be the 1st to the 15th, "
                + "or the 16th to the last day of the month (not a fixed 30th).");
        }
    }

    public String id() {
        return id;
    }

    public Cadence cadence() {
        return cadence;
    }

    public String payPeriod() {
        return cadence.name();
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public String startText() {
        return start.toString();
    }

    public String endText() {
        return end.toString();
    }

    public Status status() {
        return status;
    }

    public String createdAt() {
        return createdAt;
    }

    public String closedAt() {
        return closedAt;
    }

    public boolean acceptsRuns() {
        return status != Status.CLOSED;
    }

    public PayrollPeriod withStatus(Status next, String when) {
        return new PayrollPeriod(id, cadence, start, end, next, createdAt,
            next == Status.CLOSED ? when : closedAt);
    }

    public static Cadence cadenceFrom(String value) {
        if (value == null) throw new IllegalArgumentException("A payroll period needs a cadence.");
        switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "MONTHLY":
                return Cadence.MONTHLY;
            case "SEMI_MONTHLY":
            case "SEMI-MONTHLY":
                return Cadence.SEMI_MONTHLY;
            default:
                throw new IllegalArgumentException("Unknown pay period: " + value);
        }
    }

    public static LocalDate dateFrom(String value, String field) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must be a date like 2026-01-15.");
        }
    }

    public static Status statusFrom(String value) {
        if (value == null || value.trim().isEmpty()) return Status.OPEN;
        try {
            return Status.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown payroll period status: " + value);
        }
    }
}
