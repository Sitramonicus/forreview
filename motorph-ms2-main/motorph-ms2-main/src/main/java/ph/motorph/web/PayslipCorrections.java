package ph.motorph.web;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Correction requests for finalized payslips (filed by Payroll Staff, approved by the System Admin):
 * VOID for a payslip not yet released (it is archived and the cutoff can be finalized again as a new version);
 * ADJUST for a released payslip (the cutoff is recalculated and the gross difference goes on a later cutoff:
 * an underpayment as a taxable Adjustment earning, an overpayment as an Overpayment recovery deduction).
 */
public final class PayslipCorrections {
    public static final class Request {
        public final String id, payslipId, employeeId, kind, reason, requestedBy, requestedAt;
        public final YearMonth month, targetMonth;
        public final int cutoff, targetCutoff;
        public final String status, reviewerId, reviewedAt, outcome;
        public final long differenceCents;

        Request(String id, String payslipId, String employeeId, YearMonth month, int cutoff, String kind, YearMonth targetMonth, int targetCutoff,
                String reason, String requestedBy, String requestedAt, String status, String reviewerId, String reviewedAt, String outcome, long differenceCents) {
            this.id = id; this.payslipId = payslipId; this.employeeId = employeeId; this.month = month; this.cutoff = cutoff; this.kind = kind;
            this.targetMonth = targetMonth; this.targetCutoff = targetCutoff; this.reason = reason; this.requestedBy = requestedBy;
            this.requestedAt = requestedAt; this.status = status; this.reviewerId = reviewerId; this.reviewedAt = reviewedAt;
            this.outcome = outcome; this.differenceCents = differenceCents;
        }

        Request decided(String newStatus, String reviewer, String result, long difference) {
            return new Request(id, payslipId, employeeId, month, cutoff, kind, targetMonth, targetCutoff, reason, requestedBy, requestedAt,
                newStatus, reviewer, Instant.now().toString(), result, difference);
        }
    }

    private final Map<String, Request> requests = new LinkedHashMap<>();

    public synchronized Request create(String payslipId, String employeeId, YearMonth month, int cutoff, boolean released,
            YearMonth targetMonth, int targetCutoff, String reason, String requestedBy) {
        String clean = reason == null ? "" : reason.trim();
        if (clean.length() < 3 || clean.length() > 200) throw new IllegalArgumentException("Give a reason (3 to 200 characters).");
        for (Request existing : requests.values()) {
            if (existing.payslipId.equals(payslipId) && "Pending review".equals(existing.status)) {
                throw new IllegalStateException("This payslip already has a correction waiting for the System Admin.");
            }
        }
        if (released) {
            if (targetMonth == null || (targetCutoff != 1 && targetCutoff != 2)) throw new IllegalArgumentException("Choose the later cutoff that carries the difference.");
            boolean later = targetMonth.isAfter(month) || (targetMonth.equals(month) && targetCutoff > cutoff);
            if (!later) throw new IllegalArgumentException("The difference must go on a later cutoff than the payslip being corrected.");
        }
        Request request = new Request("PCR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT),
            payslipId, employeeId, month, cutoff, released ? "ADJUST" : "VOID", released ? targetMonth : null, released ? targetCutoff : 0,
            clean, requestedBy, Instant.now().toString(), "Pending review", "", "", "", 0);
        requests.put(request.id, request);
        return request;
    }

    public synchronized Request find(String id) {
        Request request = requests.get(id);
        if (request == null) throw new NoSuchElementException("Payslip correction was not found.");
        return request;
    }

    public synchronized Request decide(String id, String status, String reviewerId, String outcome, long difference) {
        Request current = find(id);
        if (!"Pending review".equals(current.status)) throw new IllegalStateException("Only a pending correction can be decided.");
        Request decided = current.decided(status, reviewerId, outcome, difference);
        requests.put(id, decided);
        return decided;
    }

    /** Newest first. */
    public synchronized List<Request> list() {
        List<Request> result = new ArrayList<>(requests.values());
        Collections.reverse(result);
        return result;
    }
}
