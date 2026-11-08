package motorph;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Minimal test helper (no JUnit needed). Each check prints PASS or FAIL;
 * finish() prints the totals and exits with code 1 if anything failed.
 * Run any test class in NetBeans with right-click > Run File.
 */
public final class TestSupport {

    private static int passed;
    private static int failed;

    private TestSupport() { }

    public static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    public static void check(boolean condition, String description) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + description);
        } else {
            failed++;
            System.out.println("FAIL  " + description);
        }
    }

    public static void checkEquals(Object expected, Object actual, String description) {
        boolean same = Objects.equals(expected, actual);
        check(same, description + (same ? "" : "  (expected " + expected + ", got " + actual + ")"));
    }

    /** Compares an amount after rounding it to 2 decimals (HALF_UP), the way it is displayed. */
    public static void checkAmount(String expected, BigDecimal actual, String description) {
        String shown = actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP).toPlainString();
        checkEquals(expected, shown, description);
    }

    /** Passes if the action throws IllegalArgumentException. */
    public static void checkRejects(Runnable action, String description) {
        try {
            action.run();
            check(false, description + "  (no exception thrown)");
        } catch (IllegalArgumentException e) {
            check(true, description + "  -> \"" + e.getMessage() + "\"");
        }
    }

    public static int failures() {
        return failed;
    }

    /** Prints the totals. Exits with code 1 when any check failed. */
    public static void finish() {
        System.out.println();
        System.out.println("Checks passed: " + passed + ", failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
