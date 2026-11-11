package ph.motorph.t_tkt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class PayChangeRequests {
    public static final List<String> TYPES = Collections.unmodifiableList(Arrays.asList("PROMOTION", "MERIT", "BONUS", "DISCIPLINARY"));

    public static final class Request {
        public final String id, employeeId, type, newPosition, note, requestedBy, requestedAt;
        public final BigDecimal newMonthlyBasic;
        public final YearMonth effectiveMonth, bonusMonth;
        public final int bonusCutoff;
        public final long bonusCents;
        public final LocalDate startDate, endDate;
        public final String status, reviewerId, reviewedAt, outcome;

        Request(String id, String employeeId, String type, String newPosition, BigDecimal newMonthlyBasic, YearMonth effectiveMonth,
                long bonusCents, YearMonth bonusMonth, int bonusCutoff, LocalDate startDate, LocalDate endDate, String note,
                String requestedBy, String requestedAt, String status, String reviewerId, String reviewedAt, String outcome) {
            this.id = id; this.employeeId = employeeId; this.type = type; this.newPosition = newPosition; this.newMonthlyBasic = newMonthlyBasic;
            this.effectiveMonth = effectiveMonth; this.bonusCents = bonusCents; this.bonusMonth = bonusMonth; this.bonusCutoff = bonusCutoff;
            this.startDate = startDate; this.endDate = endDate; this.note = note; this.requestedBy = requestedBy; this.requestedAt = requestedAt;
            this.status = status; this.reviewerId = reviewerId; this.reviewedAt = reviewedAt; this.outcome = outcome;
        }

        Request decided(String newStatus, String reviewer, String result) {
            return new Request(id, employeeId, type, newPosition, newMonthlyBasic, effectiveMonth, bonusCents, bonusMonth, bonusCutoff,
                startDate, endDate, note, requestedBy, requestedAt, newStatus, reviewer, Instant.now().toString(), result);
        }
    }

    
    public interface Effects {
        void promote(String employeeId, String newPosition, YearMonth effective, BigDecimal newMonthlyBasic);
        void raise(String employeeId, YearMonth effective, BigDecimal newMonthlyBasic);
        void bonus(String employeeId, YearMonth month, int cutoff, long cents, String note, String approverId);
        void unpaidLeave(String employeeId, LocalDate start, LocalDate end, String note, String approverId);
    }

    private final Map<String, Request> requests = new LinkedHashMap<>();

    
    public synchronized Request create(String employeeId, String type, String newPosition, BigDecimal newMonthlyBasic, YearMonth effectiveMonth,
            long bonusCents, YearMonth bonusMonth, int bonusCutoff, LocalDate startDate, LocalDate endDate, String note, String requestedBy) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Type must be one of " + TYPES + ".");
        String safeNote = note == null ? "" : note.trim();
        if (safeNote.length() > 200) throw new IllegalArgumentException("Note must be 200 characters or fewer.");
        if (("PROMOTION".equals(type) || "MERIT".equals(type))) {
            if (newMonthlyBasic == null || newMonthlyBasic.signum() <= 0 || newMonthlyBasic.compareTo(new BigDecimal("10000000")) > 0) {
                throw new IllegalArgumentException("New monthly basic must be more than ₱0 and at most ₱10,000,000.");
            }
            if (effectiveMonth == null) throw new IllegalArgumentException("Effective month is required.");
        }
        String position = newPosition == null ? "" : newPosition.trim();
        if ("PROMOTION".equals(type) && (position.isEmpty() || position.length() > 80)) throw new IllegalArgumentException("New position is required (80 characters max).");
        if ("BONUS".equals(type)) {
            if (bonusCents <= 0 || bonusCents > 100_000_000) throw new IllegalArgumentException("Bonus must be more than ₱0 and at most ₱1,000,000.");
            if (bonusMonth == null || (bonusCutoff != 1 && bonusCutoff != 2)) throw new IllegalArgumentException("Choose the bonus month and cutoff.");
        }
        if ("DISCIPLINARY".equals(type)) {
            if (startDate == null || endDate == null || startDate.isAfter(endDate)) throw new IllegalArgumentException("Choose valid unpaid leave dates.");
            if (safeNote.isEmpty()) throw new IllegalArgumentException("A reason is required for disciplinary unpaid leave.");
        }
        String id = "PC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        Request request = new Request(id, employeeId, type, "PROMOTION".equals(type) ? position : "",
            "PROMOTION".equals(type) || "MERIT".equals(type) ? newMonthlyBasic : null,
            "PROMOTION".equals(type) || "MERIT".equals(type) ? effectiveMonth : null,
            "BONUS".equals(type) ? bonusCents : 0, "BONUS".equals(type) ? bonusMonth : null, "BONUS".equals(type) ? bonusCutoff : 0,
            "DISCIPLINARY".equals(type) ? startDate : null, "DISCIPLINARY".equals(type) ? endDate : null,
            safeNote, requestedBy, Instant.now().toString(), "Pending review", "", "", "");
        requests.put(id, request);
        return request;
    }

    public synchronized Request find(String id) {
        Request request = requests.get(id);
        if (request == null) throw new NoSuchElementException("Pay-change request was not found.");
        return request;
    }

    
    public synchronized Request decide(String id, String decision, String reviewerId, Effects effects) {
        Request request = find(id);
        if (!"Pending review".equals(request.status)) throw new IllegalStateException("Only a pending request can be decided.");
        if ("reject".equals(decision)) return save(request.decided("Rejected", reviewerId, "No change made."));
        if (!"approve".equals(decision)) throw new IllegalArgumentException("Decision must be approve or reject.");
        String outcome;
        switch (request.type) {
            case "PROMOTION":
                effects.promote(request.employeeId, request.newPosition, request.effectiveMonth, request.newMonthlyBasic);
                outcome = "Position set to " + request.newPosition + "; monthly basic ₱" + request.newMonthlyBasic.toPlainString() + " from " + request.effectiveMonth + ".";
                break;
            case "MERIT":
                effects.raise(request.employeeId, request.effectiveMonth, request.newMonthlyBasic);
                outcome = "Monthly basic ₱" + request.newMonthlyBasic.toPlainString() + " from " + request.effectiveMonth + ".";
                break;
            case "BONUS":
                effects.bonus(request.employeeId, request.bonusMonth, request.bonusCutoff, request.bonusCents, request.note, reviewerId);
                outcome = "Taxable bonus added to the " + request.bonusMonth + " cutoff " + request.bonusCutoff + " payslip.";
                break;
            default:
                effects.unpaidLeave(request.employeeId, request.startDate, request.endDate, request.note, reviewerId);
                outcome = "Unpaid leave " + request.startDate + " to " + request.endDate + " recorded and approved.";
        }
        return save(request.decided("Approved", reviewerId, outcome));
    }

    private Request save(Request request) {
        requests.put(request.id, request);
        return request;
    }

    
    public synchronized List<Request> list() {
        List<Request> result = new ArrayList<>(requests.values());
        Collections.reverse(result);
        return result;
    }
}
