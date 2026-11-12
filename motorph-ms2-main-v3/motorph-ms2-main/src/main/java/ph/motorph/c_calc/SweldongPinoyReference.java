package ph.motorph.c_calc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Explicit, browserless adapter for the observed Sweldong Pinoy calculator form.
 *
 * This is a calibration/reference client for one narrow supported case, not a supported provider
 * API and never an automatic payroll source. A fresh cookie jar is used for every request.
 */
public final class SweldongPinoyReference {
    public static final String ENDPOINT = "https://www.sweldongpinoy.com/";
    public static final String PROVIDER_VERSION = "unknown (undocumented homepage form; captured 2026-11-02)";
    public static final String INPUT_SCOPE = "Private Employee / Monthly / 2026 only / no night differential or overtime; captured 20-day and 5-day form defaults";
    private static final int MAX_RESPONSE_BYTES = 256_000;
    private static final List<String> REMOTE_FIELDS = Arrays.asList("basic_pay", "taxable_allowance", "nontaxable_allowance",
        "night_differential", "overtime_pay", "gross_pay", "sss_gsis_contribution", "philhealth_contribution",
        "pagibig_contribution", "withholding_tax", "total_deductions", "net_pay");

    private final URI endpoint;
    private final Duration timeout;

    public SweldongPinoyReference() {
        this.endpoint = URI.create(ENDPOINT);
        this.timeout = Duration.ofSeconds(8);
    }

    /** Test seam: only the official endpoint or loopback test servers are allowed. */
    public SweldongPinoyReference(URI endpoint, Duration timeout) {
        if (endpoint == null || timeout == null || timeout.isZero() || timeout.isNegative()
                || timeout.compareTo(Duration.ofSeconds(20)) > 0) {
            throw new IllegalArgumentException("Reference endpoint and a timeout up to 20 seconds are required.");
        }
        boolean official = "https".equalsIgnoreCase(endpoint.getScheme())
            && "www.sweldongpinoy.com".equalsIgnoreCase(endpoint.getHost())
            && (endpoint.getPort() == -1 || endpoint.getPort() == 443)
            && (endpoint.getPath().isEmpty() || "/".equals(endpoint.getPath()))
            && endpoint.getRawQuery() == null && endpoint.getRawFragment() == null && endpoint.getUserInfo() == null;
        boolean loopback = "http".equalsIgnoreCase(endpoint.getScheme()) && endpoint.getHost() != null
            && ("127.0.0.1".equals(endpoint.getHost()) || "localhost".equalsIgnoreCase(endpoint.getHost()))
            && endpoint.getPort() > 0 && (endpoint.getPath().isEmpty() || "/".equals(endpoint.getPath()))
            && endpoint.getRawQuery() == null && endpoint.getRawFragment() == null && endpoint.getUserInfo() == null;
        if (!official && !loopback) throw new IllegalArgumentException("Use the official HTTPS homepage or a loopback-only test endpoint.");
        this.endpoint = endpoint;
        this.timeout = timeout;
    }

    public static Input normalize(String period, String employmentType, String payPeriod, int dailyDays,
            String basicPay, String taxableAllowance, String nontaxableAllowance, String nightHours,
            String nightRate, String overtimeHours, String overtimeRate, int workSchedule) {
        if (!"Private Employee".equals(employmentType))
            throw new IllegalArgumentException("External comparison supports Private Employee only; government/GSIS is not in scope.");
        if (!"Monthly".equals(payPeriod))
            throw new IllegalArgumentException("External comparison supports Monthly only; other period conversion is not verified.");
        YearMonth month;
        try { month = YearMonth.parse(period); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Period must use YYYY-MM."); }
        if (month.getYear() != 2026) throw new IllegalArgumentException("The local comparison ruleset covers 2026 only.");
        if (dailyDays != 20 || workSchedule != 5)
            throw new IllegalArgumentException("Only the captured 20-day / 5-day monthly form defaults are calibrated.");
        if (!quantityIsZero(nightHours) || !quantityIsZero(nightRate)
                || !quantityIsZero(overtimeHours) || !quantityIsZero(overtimeRate)) {
            throw new IllegalArgumentException("Night differential and overtime are outside the supported comparison scope.");
        }
        long basic = parseNormalizedMoney(basicPay, "Basic pay");
        long taxable = parseNormalizedMoney(taxableAllowance, "Taxable allowance");
        long nontaxable = parseNormalizedMoney(nontaxableAllowance, "Non-taxable allowance");
        if (basic <= 0) throw new IllegalArgumentException("Basic pay must be greater than zero.");
        Math.addExact(Math.addExact(basic, taxable), nontaxable);
        return new Input(month, basic, taxable, nontaxable, dailyDays, workSchedule);
    }

    private static long parseNormalizedMoney(String value, String name) {
        String text = value == null ? "" : value.trim();
        if (!text.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:[.][0-9]{1,2})?"))
            throw new IllegalArgumentException(name + " must be a peso amount with valid thousands separators and up to two decimals.");
        return StatutoryCalculator2026.parsePesoCents(text, name);
    }

    private static boolean quantityIsZero(String value) {
        if (value == null || value.trim().isEmpty()) return true;
        try {
            BigDecimal amount = new BigDecimal(value.trim().replace(",", ""));
            return amount.signum() == 0;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Premium hours and rates must be zero for this comparison.");
        }
    }

    public JsonObject compare(Input input, StatutoryRates rates) throws IOException {
        if (input == null) throw new IllegalArgumentException("Comparison input is required.");
        if (rates == null) rates = StatutoryRates.DEFAULT;
        JsonObject report = new JsonObject();
        String checkedAt = Instant.now().toString();
        report.addProperty("checkedAt", checkedAt);
        report.addProperty("comparisonStatus", "UNAVAILABLE");
        report.addProperty("scope", INPUT_SCOPE);
        report.add("input", input.toJson());
        report.add("requestPayload", requestPayload(input));

        JsonObject local = null;
        String localUnavailable = "";
        try {
            StatutoryBreakdown calculated = StatutoryCalculator2026.calculate(input.period,
                Math.addExact(Math.addExact(input.basicPayCents, input.taxableAllowanceCents), input.nontaxableAllowanceCents),
                input.basicPayCents, input.basicPayCents, input.basicPayCents,
                Math.addExact(input.basicPayCents, input.taxableAllowanceCents), rates);
            local = localResult(calculated, rates.id);
        } catch (IllegalArgumentException | ArithmeticException unavailable) {
            localUnavailable = safeReason(unavailable.getMessage());
        }

        RemoteResponse remote = request(input);
        JsonObject remoteObject = new JsonObject();
        remoteObject.addProperty("source", "Sweldong Pinoy");
        remoteObject.addProperty("endpoint", ENDPOINT);
        remoteObject.addProperty("providerVersion", PROVIDER_VERSION);
        remoteObject.addProperty("httpStatus", remote.statusCode);
        remoteObject.addProperty("contentType", remote.contentType);
        JsonArray cookieNames = new JsonArray();
        for (String name : remote.cookieNames) cookieNames.add(name);
        remoteObject.add("sessionCookieNames", cookieNames);
        remoteObject.addProperty("rawResponseText", remote.rawText);
        remoteObject.add("rawResponse", remote.rawJson.deepCopy());
        remoteObject.addProperty("validated", true);
        remoteObject.add("result", normalizedRemoteResult(remote.result));
        if (remote.altResult == null) remoteObject.add("altresult", com.google.gson.JsonNull.INSTANCE);
        else remoteObject.add("altresult", normalizedRemoteResult(remote.altResult));
        remoteObject.addProperty("altresultMatchesResult", remote.altMatches);
        remoteObject.addProperty("inputComponentsMatched", remote.inputComponentsMatched);
        report.add("sweldong", remoteObject);

        if (!remote.inputComponentsMatched) {
            throw new IOException("Sweldong returned different earnings components from the submitted monthly inputs; no comparison or fallback is offered.");
        }
        if (local != null) report.add("motorph", local);
        else report.add("motorph", com.google.gson.JsonNull.INSTANCE);
        if (!localUnavailable.isEmpty()) report.addProperty("motorphUnavailableReason", localUnavailable);

        JsonArray deltas = new JsonArray();
        boolean allMatch = local != null;
        if (local != null) {
            Map<String, Long> localCents = localCents(local);
            Map<String, Long> externalCents = remoteCents(remote.cents);
            for (String component : Arrays.asList("grossPay", "sss", "philHealth", "pagIbig", "withholdingTax", "totalDeductions", "netPay")) {
                long localValue = localCents.get(component);
                long externalValue = externalCents.get(component);
                long delta = externalValue - localValue;
                JsonObject item = new JsonObject();
                item.addProperty("component", component);
                item.addProperty("motorphCents", localValue);
                item.addProperty("sweldongCents", externalValue);
                item.addProperty("deltaCents", delta);
                item.addProperty("matches", delta == 0);
                deltas.add(item);
                allMatch &= delta == 0;
            }
        }
        report.add("deltas", deltas);
        report.addProperty("comparisonStatus", local == null ? "LOCAL_UNAVAILABLE" : allMatch ? "MATCH" : "MISMATCH");

        boolean fallbackEligible = remote.inputComponentsMatched && (remote.altResult == null || remote.altMatches);
        JsonObject fallback = new JsonObject();
        fallback.addProperty("eligible", fallbackEligible);
        fallback.addProperty("selectedSource", "MOTORPH");
        fallback.addProperty("selectionRequired", true);
        fallback.addProperty("appliesToPayroll", false);
        fallback.addProperty("reason", fallbackEligible
            ? "MotorPH remains the default. Sweldong is a validated reference only; selecting it requires a separate explicit admin decision and never changes saved payroll."
            : "Sweldong output is ambiguous or did not preserve the submitted input components; external fallback is disabled.");
        report.add("fallback", fallback);
        report.addProperty("validated", true);
        return report;
    }

    private RemoteResponse request(Input input) throws IOException {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        java.net.HttpURLConnection homepage = (java.net.HttpURLConnection) endpoint.toURL().openConnection();
        homepage.setInstanceFollowRedirects(false);
        homepage.setConnectTimeout(4_000);
        homepage.setReadTimeout((int) timeout.toMillis());
        homepage.setRequestMethod("GET");
        homepage.setRequestProperty("User-Agent", "MotorPH-Reference-Comparison/1.0");
        homepage.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        int homepageStatus;
        try {
            homepageStatus = homepage.getResponseCode();
            cookies.put(endpoint, homepage.getHeaderFields());
            if (homepageStatus < 200 || homepageStatus >= 300)
                throw new IOException("Sweldong homepage session request failed or redirected; no payroll input was sent.");
            try (InputStream body = homepage.getInputStream()) { readLimited(body); }
        } finally {
            homepage.disconnect();
        }

        String form = requestPayload(input).entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue().getAsString()))
            .collect(Collectors.joining("&"));
        java.net.HttpURLConnection post = (java.net.HttpURLConnection) endpoint.toURL().openConnection();
        post.setInstanceFollowRedirects(false);
        post.setConnectTimeout(4_000);
        post.setReadTimeout((int) timeout.toMillis());
        post.setRequestMethod("POST");
        post.setDoOutput(true);
        post.setRequestProperty("User-Agent", "MotorPH-Reference-Comparison/1.0");
        post.setRequestProperty("Accept", "application/json");
        post.setRequestProperty("Origin", origin(endpoint));
        post.setRequestProperty("Referer", endpoint.toString());
        post.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        List<String> cookieHeader = cookies.get(endpoint, java.util.Collections.emptyMap()).get("Cookie");
        if (cookieHeader != null && !cookieHeader.isEmpty()) post.setRequestProperty("Cookie", String.join("; ", cookieHeader));
        int statusCode;
        String contentType;
        byte[] bytes;
        try {
            try (java.io.OutputStream body = post.getOutputStream()) { body.write(form.getBytes(StandardCharsets.UTF_8)); }
            statusCode = post.getResponseCode();
            cookies.put(endpoint, post.getHeaderFields());
            contentType = post.getHeaderField("Content-Type");
            if (statusCode < 200 || statusCode >= 300)
                throw new IOException("Sweldong calculator request failed or redirected; no result was accepted.");
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("json"))
                throw new IOException("Sweldong did not return JSON; no result was accepted.");
            try (InputStream body = post.getInputStream()) { bytes = readLimited(body); }
        } finally {
            post.disconnect();
        }
        String raw = new String(bytes, StandardCharsets.UTF_8);
        JsonObject json;
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("not an object");
            json = parsed.getAsJsonObject();
        } catch (RuntimeException invalid) {
            throw new IOException("Sweldong returned malformed JSON; no result was accepted.");
        }
        if (!json.has("status") || !json.get("status").isJsonPrimitive()
                || !"OK".equals(json.get("status").getAsString()))
            throw new IOException("Sweldong did not return status OK; no result was accepted.");
        ValidatedResult primary = validateResult(json.get("result"), "result");
        ValidatedResult alternate = null;
        if (json.has("altresult") && !json.get("altresult").isJsonNull()) {
            if (!json.get("altresult").isJsonObject()) throw new IOException("Sweldong altresult is malformed; no result was accepted.");
            alternate = validateResult(json.get("altresult"), "altresult");
        }
        boolean same = alternate != null && primary.cents.equals(alternate.cents);
        boolean inputMatched = primary.cents.get("basic_pay") == input.basicPayCents
            && primary.cents.get("taxable_allowance") == input.taxableAllowanceCents
            && primary.cents.get("nontaxable_allowance") == input.nontaxableAllowanceCents
            && primary.cents.get("night_differential") == 0
            && primary.cents.get("overtime_pay") == 0;
        Set<String> names = new LinkedHashSet<>();
        for (HttpCookie cookie : cookies.getCookieStore().getCookies()) names.add(cookie.getName());
        return new RemoteResponse(statusCode, contentType, names, raw, json, primary.raw, alternate == null ? null : alternate.raw,
            same, inputMatched, primary.cents);
    }

    private static byte[] readLimited(InputStream body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        for (int count; (count = body.read(buffer)) != -1;) {
            total += count;
            if (total > MAX_RESPONSE_BYTES) throw new IOException("Sweldong response exceeded the safety limit; no result was accepted.");
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    private static JsonObject requestPayload(Input input) {
        JsonObject payload = new JsonObject();
        payload.addProperty("employment_type", "Private Employee");
        payload.addProperty("pay_period", "Monthly");
        payload.addProperty("payroll_period_daily_days", Integer.toString(input.dailyDays));
        payload.addProperty("basic_pay", peso(input.basicPayCents));
        payload.addProperty("taxable_allowance", peso(input.taxableAllowanceCents));
        payload.addProperty("nontaxable_allowance", peso(input.nontaxableAllowanceCents));
        payload.addProperty("night_differential_hours", "");
        payload.addProperty("night_differential_rate", "");
        payload.addProperty("overtime_hours", "");
        payload.addProperty("overtime_rate", "");
        payload.addProperty("work_schedule", Integer.toString(input.workSchedule));
        return payload;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String peso(long cents) {
        return String.format(Locale.ROOT, "%,d.%02d", cents / 100, cents % 100);
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
            && left.getHost().equalsIgnoreCase(right.getHost()) && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String origin(URI uri) {
        int port = effectivePort(uri);
        boolean standard = ("https".equalsIgnoreCase(uri.getScheme()) && port == 443)
            || ("http".equalsIgnoreCase(uri.getScheme()) && port == 80);
        return uri.getScheme() + "://" + uri.getHost() + (standard ? "" : ":" + port);
    }

    private static ValidatedResult validateResult(JsonElement element, String name) throws IOException {
        if (element == null || !element.isJsonObject()) throw new IOException("Sweldong response " + name + " is missing or malformed.");
        JsonObject raw = element.getAsJsonObject().deepCopy();
        Map<String, Long> values = new LinkedHashMap<>();
        try {
            for (String field : REMOTE_FIELDS) {
                if (!raw.has(field) || raw.get(field).isJsonNull()) throw new IllegalArgumentException();
                values.put(field, cents(raw.get(field)));
            }
        } catch (RuntimeException invalid) {
            throw new IOException("Sweldong response " + name + " is missing valid monetary fields.");
        }
        long grossParts = values.get("basic_pay") + values.get("taxable_allowance") + values.get("nontaxable_allowance")
            + values.get("night_differential") + values.get("overtime_pay");
        long deductions = values.get("sss_gsis_contribution") + values.get("philhealth_contribution")
            + values.get("pagibig_contribution") + values.get("withholding_tax");
        if (Math.abs(values.get("gross_pay") - grossParts) > 1)
            throw new IOException("Sweldong response " + name + " does not balance gross pay against its components.");
        if (Math.abs(values.get("total_deductions") - deductions) > 1)
            throw new IOException("Sweldong response " + name + " does not balance deductions against its components.");
        if (Math.abs(values.get("net_pay") - (values.get("gross_pay") - values.get("total_deductions"))) > 1)
            throw new IOException("Sweldong response " + name + " does not balance net pay.");
        return new ValidatedResult(raw, values);
    }

    private static long cents(JsonElement raw) {
        BigDecimal money = new BigDecimal(raw.getAsString().replace(",", "")).setScale(2, RoundingMode.HALF_UP);
        if (money.signum() < 0 || money.compareTo(new BigDecimal("10000000000.00")) > 0)
            throw new IllegalArgumentException("money outside range");
        return money.movePointRight(2).longValueExact();
    }

    private static JsonObject normalizedRemoteResult(JsonObject raw) {
        JsonObject result = new JsonObject();
        for (String field : REMOTE_FIELDS) result.addProperty(field, peso(cents(raw.get(field))));
        return result;
    }

    private static JsonObject localResult(StatutoryBreakdown result, String ratesVersion) {
        JsonObject json = new JsonObject();
        json.addProperty("source", "MotorPH");
        json.addProperty("rulesetId", result.rulesetId);
        json.addProperty("ratesVersion", ratesVersion);
        json.addProperty("period", result.period.toString());
        json.addProperty("grossPayCents", result.grossCashPayCents);
        json.addProperty("sssCents", result.sssEmployeeCents);
        json.addProperty("philHealthCents", result.philHealthEmployeeCents);
        json.addProperty("pagIbigCents", result.pagIbigEmployeeCents);
        json.addProperty("withholdingTaxCents", result.withholdingTaxCents);
        json.addProperty("totalDeductionsCents", result.totalEmployeeDeductionsCents());
        json.addProperty("netPayCents", result.netPayCents());
        return json;
    }

    private static Map<String, Long> localCents(JsonObject json) {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("grossPay", json.get("grossPayCents").getAsLong());
        values.put("sss", json.get("sssCents").getAsLong());
        values.put("philHealth", json.get("philHealthCents").getAsLong());
        values.put("pagIbig", json.get("pagIbigCents").getAsLong());
        values.put("withholdingTax", json.get("withholdingTaxCents").getAsLong());
        values.put("totalDeductions", json.get("totalDeductionsCents").getAsLong());
        values.put("netPay", json.get("netPayCents").getAsLong());
        return values;
    }

    private static Map<String, Long> remoteCents(Map<String, Long> values) {
        Map<String, Long> mapped = new LinkedHashMap<>();
        mapped.put("grossPay", values.get("gross_pay"));
        mapped.put("sss", values.get("sss_gsis_contribution"));
        mapped.put("philHealth", values.get("philhealth_contribution"));
        mapped.put("pagIbig", values.get("pagibig_contribution"));
        mapped.put("withholdingTax", values.get("withholding_tax"));
        mapped.put("totalDeductions", values.get("total_deductions"));
        mapped.put("netPay", values.get("net_pay"));
        return mapped;
    }

    private static String safeReason(String value) {
        if (value == null || value.trim().isEmpty()) return "MotorPH estimate unavailable for this input.";
        String cleaned = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return cleaned.length() > 240 ? cleaned.substring(0, 240) : cleaned;
    }

    public static final class Input {
        public final YearMonth period;
        public final long basicPayCents, taxableAllowanceCents, nontaxableAllowanceCents;
        public final int dailyDays, workSchedule;

        private Input(YearMonth period, long basic, long taxable, long nontaxable, int dailyDays, int workSchedule) {
            this.period = period;
            this.basicPayCents = basic;
            this.taxableAllowanceCents = taxable;
            this.nontaxableAllowanceCents = nontaxable;
            this.dailyDays = dailyDays;
            this.workSchedule = workSchedule;
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("employmentType", "Private Employee");
            json.addProperty("payPeriod", "Monthly");
            json.addProperty("period", period.toString());
            json.addProperty("payrollPeriodDailyDays", dailyDays);
            json.addProperty("basicPayCents", basicPayCents);
            json.addProperty("basicPay", peso(basicPayCents));
            json.addProperty("taxableAllowanceCents", taxableAllowanceCents);
            json.addProperty("taxableAllowance", peso(taxableAllowanceCents));
            json.addProperty("nontaxableAllowanceCents", nontaxableAllowanceCents);
            json.addProperty("nontaxableAllowance", peso(nontaxableAllowanceCents));
            json.addProperty("nightDifferentialHours", "");
            json.addProperty("overtimeHours", "");
            json.addProperty("workSchedule", workSchedule);
            return json;
        }
    }

    private static final class ValidatedResult {
        final JsonObject raw;
        final Map<String, Long> cents;
        ValidatedResult(JsonObject raw, Map<String, Long> cents) { this.raw = raw; this.cents = cents; }
    }

    private static final class RemoteResponse {
        final int statusCode;
        final String contentType, rawText;
        final Set<String> cookieNames;
        final JsonObject rawJson, result, altResult;
        final boolean altMatches, inputComponentsMatched;
        final Map<String, Long> cents;
        RemoteResponse(int statusCode, String contentType, Set<String> cookieNames, String rawText, JsonObject rawJson,
                JsonObject result, JsonObject altResult, boolean altMatches, boolean inputComponentsMatched, Map<String, Long> cents) {
            this.statusCode = statusCode; this.contentType = contentType; this.cookieNames = cookieNames;
            this.rawText = rawText; this.rawJson = rawJson; this.result = result; this.altResult = altResult;
            this.altMatches = altMatches; this.inputComponentsMatched = inputComponentsMatched; this.cents = cents;
        }
    }
}
