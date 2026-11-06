package ph.motorph.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Government numbers (SSS, PhilHealth, Pag-IBIG MID, TIN) per employee, local demo, synthetic values only.
 * Matrix: System Admin full access; HR and Payroll "A" (approval required). HR/Payroll see masked numbers, propose
 * changes and request full view; the System Admin approves both (a full-view grant lasts until the end of that day).
 * Every full view, change and decision is written to the audit log. Who may do what is in AccessPolicy.
 */
public final class GovernmentDetails {
    /** Field name, digits allowed, display pattern (# = digit). */
    public static final List<String[]> FIELDS = Collections.unmodifiableList(Arrays.asList(
        new String[] {"sss", "10", "##-#######-#", "SSS number"},
        new String[] {"philHealth", "12", "##-#########-#", "PhilHealth number"},
        new String[] {"pagIbig", "12", "####-####-####", "Pag-IBIG MID"},
        new String[] {"tin", "9|12", "###-###-###|###-###-###-###", "TIN"}));

    public static final class Numbers {
        public final Map<String, String> digits; // field -> digits only ("" = not on file)
        public final String updatedBy, updatedAt;

        Numbers(Map<String, String> digits, String updatedBy, String updatedAt) {
            this.digits = Collections.unmodifiableMap(new LinkedHashMap<>(digits));
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
        }
    }

    public static final class Request {
        public final String id, kind, employeeId, requestedBy, requestedAt, reason;
        public final Map<String, String> proposed; // CHANGE only; digits
        public final String status, reviewerId, reviewedAt;
        public final LocalDate accessDate; // ACCESS grant day (full view until the end of it)

        Request(String id, String kind, String employeeId, Map<String, String> proposed, String reason, String requestedBy, String requestedAt,
                String status, String reviewerId, String reviewedAt, LocalDate accessDate) {
            this.id = id; this.kind = kind; this.employeeId = employeeId; this.proposed = Collections.unmodifiableMap(new LinkedHashMap<>(proposed));
            this.reason = reason; this.requestedBy = requestedBy; this.requestedAt = requestedAt; this.status = status;
            this.reviewerId = reviewerId; this.reviewedAt = reviewedAt; this.accessDate = accessDate;
        }
    }

    public static final class AuditEntry {
        public final String at, actorId, employeeId, action, detail;

        AuditEntry(String actorId, String employeeId, String action, String detail) {
            this.at = Instant.now().toString(); this.actorId = actorId; this.employeeId = employeeId; this.action = action; this.detail = detail;
        }
    }

    private final Map<String, Numbers> numbers = new LinkedHashMap<>();
    private final Map<String, Request> requests = new LinkedHashMap<>();
    private final List<AuditEntry> audit = new ArrayList<>();

    /** Digits only; "" clears; otherwise the length must match the field (TIN 9 or 12 digits). */
    public static String normalize(String field, String value) {
        String[] spec = spec(field);
        String digits = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (digits.isEmpty()) return "";
        if (!digits.matches("[0-9]+") || !Arrays.asList(spec[1].split("\\|")).contains(String.valueOf(digits.length()))) {
            throw new IllegalArgumentException(spec[3] + " must have " + spec[1].replace("|", " or ") + " digits (" + spec[2].replace("|", " or ").replace('#', '0') + ").");
        }
        return digits;
    }

    /** Formatted with dashes; masked keeps only the last 4 digits. */
    public static String display(String field, String digits, boolean masked) {
        if (digits == null || digits.isEmpty()) return "";
        String pattern = "";
        for (String option : spec(field)[2].split("\\|")) if (option.replace("-", "").length() == digits.length()) pattern = option;
        StringBuilder out = new StringBuilder();
        int index = 0;
        for (char c : pattern.toCharArray()) {
            if (c != '#') {
                out.append(c);
                continue;
            }
            out.append(masked && index < digits.length() - 4 ? '•' : digits.charAt(index));
            index++;
        }
        return out.toString();
    }

    private static String[] spec(String field) {
        for (String[] spec : FIELDS) if (spec[0].equals(field)) return spec;
        throw new IllegalArgumentException("Unknown government field: " + field);
    }

    private static Map<String, String> normalizeAll(Map<String, String> input) {
        Map<String, String> clean = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) clean.put(entry.getKey(), normalize(entry.getKey(), entry.getValue()));
        return clean;
    }

    public synchronized Numbers get(String employeeId) {
        Numbers current = numbers.get(employeeId);
        if (current != null) return current;
        Map<String, String> empty = new LinkedHashMap<>();
        for (String[] spec : FIELDS) empty.put(spec[0], "");
        return new Numbers(empty, "", "");
    }

    /** System Admin: immediate edit. */
    public synchronized Numbers set(String employeeId, Map<String, String> fields, String by) {
        Map<String, String> merged = new LinkedHashMap<>(get(employeeId).digits);
        merged.putAll(normalizeAll(fields));
        Numbers saved = new Numbers(merged, by, Instant.now().toString());
        numbers.put(employeeId, saved);
        log(by, employeeId, "EDITED", "Changed " + String.join(", ", fields.keySet()) + " directly");
        return saved;
    }

    public synchronized Request proposeChange(String employeeId, Map<String, String> fields, String reason, String by) {
        if (fields.isEmpty()) throw new IllegalArgumentException("Propose at least one number.");
        Request request = new Request("GD-" + suffix(), "CHANGE", employeeId, normalizeAll(fields), text(reason), by, Instant.now().toString(),
            "Pending review", "", "", null);
        requests.put(request.id, request);
        log(by, employeeId, "CHANGE_REQUESTED", "Proposed " + String.join(", ", fields.keySet()));
        return request;
    }

    public synchronized Request requestAccess(String employeeId, String reason, String by) {
        String clean = text(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Say why you need the full numbers.");
        Request request = new Request("GA-" + suffix(), "ACCESS", employeeId, Collections.emptyMap(), clean, by, Instant.now().toString(),
            "Pending review", "", "", null);
        requests.put(request.id, request);
        log(by, employeeId, "ACCESS_REQUESTED", clean);
        return request;
    }

    /** Approving a change applies it; approving access grants full view for the rest of {@code today}. */
    public synchronized Request decide(String id, String decision, String reviewerId, LocalDate today) {
        Request current = find(id);
        if (!"Pending review".equals(current.status)) throw new IllegalStateException("Only a pending request can be decided.");
        boolean approve = "approve".equals(decision);
        if (!approve && !"reject".equals(decision)) throw new IllegalArgumentException("Decision must be approve or reject.");
        if (approve && "CHANGE".equals(current.kind)) {
            Map<String, String> merged = new LinkedHashMap<>(get(current.employeeId).digits);
            merged.putAll(current.proposed);
            numbers.put(current.employeeId, new Numbers(merged, current.requestedBy + " (approved by " + reviewerId + ")", Instant.now().toString()));
        }
        Request decided = new Request(current.id, current.kind, current.employeeId, current.proposed, current.reason, current.requestedBy,
            current.requestedAt, approve ? "Approved" : "Rejected", reviewerId, Instant.now().toString(), approve && "ACCESS".equals(current.kind) ? today : null);
        requests.put(id, decided);
        log(reviewerId, current.employeeId, current.kind + (approve ? "_APPROVED" : "_REJECTED"), current.id + " from " + current.requestedBy);
        return decided;
    }

    /** True when an approved full-view grant for this person and day exists. */
    public synchronized boolean hasAccess(String viewerId, String employeeId, LocalDate today) {
        for (Request request : requests.values()) {
            if ("ACCESS".equals(request.kind) && "Approved".equals(request.status) && request.requestedBy.equals(viewerId)
                    && request.employeeId.equals(employeeId) && today.equals(request.accessDate)) return true;
        }
        return false;
    }

    public synchronized void logFullView(String viewerId, String employeeId, String basis) {
        log(viewerId, employeeId, "VIEWED_FULL", basis);
    }

    public synchronized Request find(String id) {
        Request request = requests.get(id);
        if (request == null) throw new NoSuchElementException("Government details request was not found.");
        return request;
    }

    /** Newest first. */
    public synchronized List<Request> requests() {
        List<Request> result = new ArrayList<>(requests.values());
        Collections.reverse(result);
        return result;
    }

    /** Newest first. */
    public synchronized List<AuditEntry> audit() {
        List<AuditEntry> result = new ArrayList<>(audit);
        Collections.reverse(result);
        return result;
    }

    private void log(String actorId, String employeeId, String action, String detail) {
        audit.add(new AuditEntry(actorId, employeeId, action, detail));
    }

    private static String text(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() > 200) throw new IllegalArgumentException("Reason must be 200 characters or fewer.");
        return clean;
    }

    private static String suffix() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT); }
}
