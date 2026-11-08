package ph.motorph.c_calc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Design doc ("System generates PDFs for each employee"): a one-page, text-only PDF of a finalized payslip snapshot.
 * Written by hand (PDF 1.4, built-in Helvetica) so no library is needed. Amounts say "PHP" because the standard
 * PDF fonts have no peso sign.
 * shortcut: plain text lines, no table layout or logo; use a PDF library (e.g. OpenPDF) for a designed payslip.
 */
public final class PayslipPdf {
    private PayslipPdf() { }

    static String money(long cents) {
        return String.format(java.util.Locale.ROOT, "PHP %,.2f", java.math.BigDecimal.valueOf(cents, 2));
    }

    /** The text lines printed on the payslip. */
    static List<String> lines(JsonObject slip) {
        JsonObject person = slip.getAsJsonObject("employee"), period = slip.getAsJsonObject("period");
        JsonObject d = slip.getAsJsonObject("deductions"), a = slip.getAsJsonObject("allowances");
        List<String> lines = new ArrayList<>();
        lines.add("MotorPH Payroll Office - Payslip (synthetic demo data)");
        lines.add("");
        lines.add("Payslip " + slip.get("payslipId").getAsString() + (slip.has("version") ? "   version " + slip.get("version").getAsString() : ""));
        lines.add("Employee: " + person.get("id").getAsString() + " - " + person.get("name").getAsString());
        lines.add("Department: " + person.get("department").getAsString() + "   Position: " + person.get("role").getAsString());
        lines.add("Period: " + period.get("from").getAsString() + " to " + period.get("to").getAsString() + "   Hourly rate: PHP " + slip.get("hourlyRate").getAsString());
        lines.add("");
        lines.add("Earned pay ............................ " + money(slip.get("earnedPayCents").getAsLong()));
        for (JsonElement bonus : slip.getAsJsonArray("bonuses")) {
            JsonObject b = bonus.getAsJsonObject();
            lines.add("  " + b.get("type").getAsString() + " ...................... " + money(b.get("amountCents").getAsLong()));
        }
        lines.add("GROSS PAY ............................. " + money(slip.get("grossPayCents").getAsLong()));
        lines.add("  SSS ................................. -" + money(d.get("sssCents").getAsLong()));
        lines.add("  PhilHealth .......................... -" + money(d.get("philHealthCents").getAsLong()));
        lines.add("  Pag-IBIG ............................ -" + money(d.get("pagIbigCents").getAsLong()));
        lines.add("  Withholding tax ..................... -" + money(d.get("withholdingTaxCents").getAsLong()));
        JsonArray voluntary = d.has("voluntary") ? d.getAsJsonArray("voluntary") : new JsonArray();
        for (JsonElement item : voluntary) {
            JsonObject v = item.getAsJsonObject();
            lines.add("  " + v.get("type").getAsString() + " ...................... -" + money(v.get("amountCents").getAsLong()));
        }
        lines.add("Allowances (non-taxable) .............. +" + money(a.get("totalCents").getAsLong()));
        lines.add("NET PAY ............................... " + money(slip.get("netPayCents").getAsLong()));
        lines.add("");
        lines.add("Pay method: " + (slip.has("payMethod") ? slip.get("payMethod").getAsString() : "-"));
        lines.add("Rates: " + slip.get("rulesetId").getAsString() + "   Finalized: " + (slip.has("finalizedAt") ? slip.get("finalizedAt").getAsString() : "-"));
        if (slip.has("voided") && slip.get("voided").getAsBoolean()) lines.add("VOIDED - kept for the record; this is not the payslip in force.");
        return lines;
    }

    public static byte[] render(JsonObject slip) { return renderLines(lines(slip)); }

    static final int LINES_PER_PAGE = 54;

    /** Text lines on as many A4 pages as needed (Helvetica 10 pt, 14 pt leading). */
    public static byte[] renderLines(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < Math.max(1, lines.size()); i += LINES_PER_PAGE) pages.add(lines.subList(i, Math.min(lines.size(), i + LINES_PER_PAGE)));
        // Objects: 1 catalog, 2 page tree, 3 font, then a page and its content stream for each page.
        List<String> objects = new ArrayList<>();
        StringBuilder kids = new StringBuilder();
        for (int k = 0; k < pages.size(); k++) kids.append(4 + 2 * k).append(" 0 R ");
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pages.size() + " >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        List<byte[]> streams = new ArrayList<>();
        for (int k = 0; k < pages.size(); k++) {
            StringBuilder text = new StringBuilder("BT /F1 10 Tf 50 790 Td 14 TL\n");
            for (String line : pages.get(k)) text.append('(').append(escape(line)).append(") Tj T*\n");
            text.append("ET");
            streams.add(text.toString().getBytes(StandardCharsets.ISO_8859_1));
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + (5 + 2 * k) + " 0 R >>");
            objects.add(null);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");
        int stream = 0;
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            if (objects.get(i) != null) {
                write(out, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
            } else {
                byte[] bytes = streams.get(stream++);
                write(out, (i + 1) + " 0 obj\n<< /Length " + bytes.length + " >>\nstream\n");
                out.write(bytes, 0, bytes.length);
                write(out, "\nendstream\nendobj\n");
            }
        }
        int xref = out.size();
        StringBuilder table = new StringBuilder("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) table.append(String.format("%010d 00000 n \n", offset));
        table.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        write(out, table.toString());
        return out.toByteArray();
    }

    /** PDF string escaping; anything outside Latin-1 becomes '?'. */
    static String escape(String value) {
        StringBuilder safe = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') safe.append('\\').append(c);
            else if (c < 32 || c > 255) safe.append('?');
            else safe.append(c);
        }
        return safe.toString();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes, 0, bytes.length);
    }
}
