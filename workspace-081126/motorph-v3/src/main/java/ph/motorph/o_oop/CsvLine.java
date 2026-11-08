package ph.motorph.o_oop;

import java.util.ArrayList;
import java.util.List;

/**
 * A single CSV record reader that understands quoted fields, embedded commas and doubled quotes.
 *
 * MotorPH's file needs all three: addresses look like {@code "Valero Carpark Building Valero Street 1227,
 * Makati City"} and the supervisor column contains names such as {@code "Garcia, Manuel III"}. A naive
 * {@code split(",")} turns one employee into three.
 */
public final class CsvLine {
    private CsvLine() {}

    public static List<String> fields(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else quoted = false;
                } else cell.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cell.toString());
                cell.setLength(0);
            } else if (c != '\r') {
                cell.append(c);
            }
        }
        out.add(cell.toString());
        return out;
    }

    /** Money as the file writes it: "53,500" or "1,500.00" → 53500.00. Blank becomes zero. */
    public static java.math.BigDecimal money(String raw) {
        String cleaned = raw == null ? "" : raw.replaceAll("[^0-9.\\-]", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-")) return java.math.BigDecimal.ZERO;
        return new java.math.BigDecimal(cleaned).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Money the way a person reads it: 2000.00 -> "2,000.00". Used for messages, never for arithmetic. */
    public static String moneyText(java.math.BigDecimal amount) {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amount);
    }

    /** Dates arrive as M/D/YYYY in the file. */
    public static java.time.LocalDate date(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return null;
        String[] parts = text.split("[/-]");
        if (parts.length != 3) return null;
        try {
            int month = Integer.parseInt(parts[0]), day = Integer.parseInt(parts[1]), year = Integer.parseInt(parts[2]);
            return java.time.LocalDate.of(year, month, day);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
