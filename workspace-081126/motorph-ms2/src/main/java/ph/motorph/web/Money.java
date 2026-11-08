package ph.motorph.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Exact-decimal money helper. Every payroll amount passes through here so rounding
 * happens in exactly one place. Firestore has no decimal type, so amounts are stored
 * as plain decimal strings and parsed back into BigDecimal.
 */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() { }

    /** Two-decimal, half-up: the convention that reproduces the supplied MotorPH payslip. */
    public static BigDecimal round(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("amount must not be null");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Parse a user/CSV amount, tolerating currency marks, spaces and thousands separators. */
    public static BigDecimal parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("amount must not be null");
        String cleaned = raw.replace('\u20B1', ' ').replace(",", "").replace("\u00A0", " ").trim();
        if (cleaned.isEmpty()) throw new IllegalArgumentException("amount must not be blank");
        try {
            return round(new BigDecimal(cleaned));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Not a valid amount: " + raw.trim());
        }
    }

    /** Parse, treating blank as zero; used for optional allowance columns. */
    public static BigDecimal parseOrZero(String raw) {
        return raw == null || raw.trim().isEmpty() ? ZERO : parse(raw);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    public static BigDecimal percentOf(BigDecimal base, String percent) {
        return round(base.multiply(new BigDecimal(percent)).divide(HUNDRED, 10, RoundingMode.HALF_UP));
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) { return a.compareTo(b) <= 0 ? a : b; }
    public static BigDecimal max(BigDecimal a, BigDecimal b) { return a.compareTo(b) >= 0 ? a : b; }

    public static boolean isNegative(BigDecimal value) { return value.compareTo(BigDecimal.ZERO) < 0; }

    /** Thousands-separated two-decimal text for display only; stored and compared values stay BigDecimal. */
    public static String format(BigDecimal value) {
        return String.format(Locale.ROOT, "%,.2f", round(value));
    }
}
