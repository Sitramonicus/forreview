package ph.motorph.o_oop;

import java.util.ArrayList;
import java.util.List;

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
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cell.append('"'); i++; } // NOPMD the escaped pair skips a char
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

    
    public static java.math.BigDecimal money(String raw) {
        String cleaned = raw == null ? "" : raw.replaceAll("[^0-9.\\-]", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-")) return java.math.BigDecimal.ZERO;
        return new java.math.BigDecimal(cleaned).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    
    public static String moneyText(java.math.BigDecimal amount) {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amount);
    }

    
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
