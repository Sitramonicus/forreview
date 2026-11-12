package ph.motorph.o_oop;

import com.google.gson.annotations.SerializedName;
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

public final class GovernmentDetails {
    
    public static final List<String[]> FIELDS = Collections.unmodifiableList(Arrays.asList(
        new String[] {"sss", "10", "##-#######-#", "SSS number"},
        new String[] {"philHealth", "12", "##-#########-#", "PhilHealth number"},
        new String[] {"pagIbig", "12", "####-####-####", "Pag-IBIG MID"},
        new String[] {"tin", "9|12", "###-###-###|###-###-###-###", "TIN"}));

    public static final class Numbers {
        @SerializedName("digits") public final Map<String, String> gd_mDigits;
        @SerializedName("updatedBy") public final String gd_sUpdatedBy;
        @SerializedName("updatedAt") public final String gd_sUpdatedAt;

        Numbers(Map<String, String> digits, String updatedBy, String updatedAt) {
            this.gd_mDigits = Collections.unmodifiableMap(new LinkedHashMap<>(digits));
            this.gd_sUpdatedBy = updatedBy;
            this.gd_sUpdatedAt = updatedAt;
        }
    }

    public static final class Request {
        @SerializedName("id") public final String gd_sId;
        @SerializedName("kind") public final String gd_sKind;
        @SerializedName("employeeId") public final String gd_sEid;
        @SerializedName("requestedBy") public final String gd_sReqBy;
        @SerializedName("requestedAt") public final String gd_sReqAt;
        @SerializedName("reason") public final String gd_sReason;
        @SerializedName("proposed") public final Map<String, String> gd_mProposed;
        @SerializedName("status") public final String gd_sStatus;
        @SerializedName("reviewerId") public final String gd_sRevId;
        @SerializedName("reviewedAt") public final String gd_sRevAt;
        @SerializedName("accessDate") public final LocalDate gd_dAccess;

        Request(String id, String kind, String employeeId, Map<String, String> proposed, String reason, String requestedBy, String requestedAt,
                String status, String reviewerId, String reviewedAt, LocalDate accessDate) {
            this.gd_sId = id; this.gd_sKind = kind; this.gd_sEid = employeeId; this.gd_mProposed = Collections.unmodifiableMap(new LinkedHashMap<>(proposed));
            this.gd_sReason = reason; this.gd_sReqBy = requestedBy; this.gd_sReqAt = requestedAt; this.gd_sStatus = status;
            this.gd_sRevId = reviewerId; this.gd_sRevAt = reviewedAt; this.gd_dAccess = accessDate;
        }
    }

    public static final class AuditEntry {
        @SerializedName("at") public final String gd_sAt;
        @SerializedName("actorId") public final String gd_sActor;
        @SerializedName("employeeId") public final String gd_sEid;
        @SerializedName("action") public final String gd_sAction;
        @SerializedName("detail") public final String gd_sDetail;

        AuditEntry(String actorId, String employeeId, String action, String detail) {
            this.gd_sAt = Instant.now().toString(); this.gd_sActor = actorId; this.gd_sEid = employeeId;
            this.gd_sAction = action; this.gd_sDetail = detail;
        }
    }

    private final Map<String, Numbers> numbers = new LinkedHashMap<>();
    private final Map<String, Request> requests = new LinkedHashMap<>();
    private final List<AuditEntry> audit = new ArrayList<>();

    
    public static String normalize(String field, String value) {
        String[] spec = spec(field);
        String digits = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (digits.isEmpty()) return "";
        if (!digits.matches("[0-9]+") || !Arrays.asList(spec[1].split("\\|")).contains(String.valueOf(digits.length()))) {
            throw new IllegalArgumentException(spec[3] + " must have " + spec[1].replace("|", " or ") + " digits (" + spec[2].replace("|", " or ").replace('#', '0') + ").");
        }
        return digits;
    }

    
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

    
    public synchronized Numbers set(String employeeId, Map<String, String> fields, String by) {
        Map<String, String> merged = new LinkedHashMap<>(get(employeeId).gd_mDigits);
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
        requests.put(request.gd_sId, request);
        log(by, employeeId, "CHANGE_REQUESTED", "Proposed " + String.join(", ", fields.keySet()));
        return request;
    }

    public synchronized Request requestAccess(String employeeId, String reason, String by) {
        String clean = text(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Say why you need the full numbers.");
        Request request = new Request("GA-" + suffix(), "ACCESS", employeeId, Collections.emptyMap(), clean, by, Instant.now().toString(),
            "Pending review", "", "", null);
        requests.put(request.gd_sId, request);
        log(by, employeeId, "ACCESS_REQUESTED", clean);
        return request;
    }

    
    public synchronized Request decide(String id, String decision, String reviewerId, LocalDate today) {
        Request current = find(id);
        if (!"Pending review".equals(current.gd_sStatus)) throw new IllegalStateException("Only a pending request can be decided.");
        boolean approve = "approve".equals(decision);
        if (!approve && !"reject".equals(decision)) throw new IllegalArgumentException("Decision must be approve or reject.");
        if (approve && "CHANGE".equals(current.gd_sKind)) {
            Map<String, String> merged = new LinkedHashMap<>(get(current.gd_sEid).gd_mDigits);
            merged.putAll(current.gd_mProposed);
            numbers.put(current.gd_sEid, new Numbers(merged, current.gd_sReqBy + " (approved by " + reviewerId + ")", Instant.now().toString()));
        }
        Request decided = new Request(current.gd_sId, current.gd_sKind, current.gd_sEid, current.gd_mProposed, current.gd_sReason, current.gd_sReqBy,
            current.gd_sReqAt, approve ? "Approved" : "Rejected", reviewerId, Instant.now().toString(), approve && "ACCESS".equals(current.gd_sKind) ? today : null);
        requests.put(id, decided);
        log(reviewerId, current.gd_sEid, current.gd_sKind + (approve ? "_APPROVED" : "_REJECTED"), current.gd_sId + " from " + current.gd_sReqBy);
        return decided;
    }

    
    public synchronized boolean hasAccess(String viewerId, String employeeId, LocalDate today) {
        for (Request request : requests.values()) {
            if ("ACCESS".equals(request.gd_sKind) && "Approved".equals(request.gd_sStatus) && request.gd_sReqBy.equals(viewerId)
                    && request.gd_sEid.equals(employeeId) && today.equals(request.gd_dAccess)) return true;
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

    
    public synchronized List<Request> requests() {
        List<Request> result = new ArrayList<>(requests.values());
        Collections.reverse(result);
        return result;
    }

    
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
