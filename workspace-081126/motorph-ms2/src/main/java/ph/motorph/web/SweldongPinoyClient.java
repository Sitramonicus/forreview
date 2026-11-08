package ph.motorph.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional, read-only comparison against the public Sweldong Pinoy calculator.
 *
 * This exists so a payroll clerk can see our figures and an independent calculator's figures side by
 * side. It is deliberately NOT part of the calculation path:
 *
 *  - the payslip is computed by {@link PayrollService} and never by this class;
 *  - every failure (timeout, network, malformed answer, their site changed) returns an unavailable
 *    result instead of throwing, so a comparison can never break payroll;
 *  - it runs server-side so salary data is never posted to a third party from the user's browser.
 *
 * The endpoint is an undocumented form POST that answers JSON. Because it is undocumented, this class
 * treats it as best-effort: a short timeout, no retries, no caching, and a visible "reference only"
 * label in the response.
 */
public final class SweldongPinoyClient {

    /** Undocumented internal endpoint. Overridable so tests never touch the real site. */
    static final String DEFAULT_ENDPOINT = "https://www.sweldongpinoy.com/";
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final String endpoint;
    private final HttpClient http;

    public SweldongPinoyClient() {
        this(DEFAULT_ENDPOINT);
    }

    SweldongPinoyClient(String endpoint) {
        this.endpoint = endpoint == null || endpoint.trim().isEmpty() ? DEFAULT_ENDPOINT : endpoint.trim();
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(
            HttpClient.Redirect.NORMAL).build();
    }

    /** What the comparison produced. Never throws; "available" is false when their site failed. */
    public static final class Comparison {
        public final boolean available;
        public final String reason;
        public final JsonObject theirs;

        private Comparison(boolean available, String reason, JsonObject theirs) {
            this.available = available;
            this.reason = reason;
            this.theirs = theirs;
        }

        static Comparison unavailable(String reason) {
            return new Comparison(false, reason, null);
        }

        static Comparison available(JsonObject theirs) {
            return new Comparison(true, "", theirs);
        }
    }

    /**
     * Asks the reference calculator for one pay period.
     *
     * @param payPeriod "MONTHLY" or "SEMI_MONTHLY"
     * @param basic     the basic pay to quote; for a semi-monthly cutoff pass the cutoff amount
     * @param taxableAllowance    allowance the reference site taxes
     * @param nonTaxableAllowance allowance it does not tax
     * @param daysWorked          used only for their "Daily" period
     */
    public Comparison compare(String payPeriod, BigDecimal basic, BigDecimal taxableAllowance,
                              BigDecimal nonTaxableAllowance, BigDecimal daysWorked,
                              BigDecimal overtimeHours, BigDecimal nightDifferentialHours) {
        try {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("employment_type", "Private Employee");
            form.put("pay_period", referencePeriod(payPeriod));
            form.put("payroll_period_daily_days", plain(daysWorked == null ? BigDecimal.valueOf(20) : daysWorked));
            form.put("basic_pay", money(basic));
            form.put("taxable_allowance", money(taxableAllowance));
            form.put("nontaxable_allowance", money(nonTaxableAllowance));
            form.put("night_differential_hours", hours(nightDifferentialHours));
            form.put("night_differential_rate", hours(nightDifferentialHours).isEmpty() ? "" : "10");
            form.put("overtime_hours", hours(overtimeHours));
            form.put("overtime_rate", hours(overtimeHours).isEmpty() ? "" : "125");
            form.put("work_schedule", "5");

            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> entry : form.entrySet()) {
                if (body.length() > 0) body.append('&');
                body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .header("User-Agent", "MotorPH-Payroll-Comparison/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Comparison.unavailable("Reference calculator answered " + response.statusCode() + ".");
            }
            JsonElement parsed = JsonParser.parseString(response.body());
            if (!parsed.isJsonObject()) return Comparison.unavailable("Reference calculator returned no result.");
            JsonObject root = parsed.getAsJsonObject();
            JsonObject result = root.has("result") && root.get("result").isJsonObject()
                ? root.getAsJsonObject("result") : root;
            if (!result.has("net_pay")) return Comparison.unavailable("Reference calculator returned no figures.");
            return Comparison.available(result);
        } catch (java.net.http.HttpTimeoutException ex) {
            return Comparison.unavailable("Reference calculator timed out.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Comparison.unavailable("Reference calculation was interrupted.");
        } catch (IOException | IllegalArgumentException | com.google.gson.JsonParseException ex) {
            return Comparison.unavailable("Reference calculator could not be reached.");
        }
    }

    /** Their field names, kept in one place so a site change is a one-line edit. */
    static String referencePeriod(String payPeriod) {
        return "SEMI_MONTHLY".equalsIgnoreCase(payPeriod) ? "Semi-Monthly" : "Monthly";
    }

    private static String money(BigDecimal value) {
        return String.format(java.util.Locale.ROOT, "%,.2f", value == null ? BigDecimal.ZERO : value);
    }

    private static String hours(BigDecimal value) {
        if (value == null || value.signum() == 0) return "";
        return plain(value);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
