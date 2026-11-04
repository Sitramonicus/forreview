package Backend;

import java.util.ArrayList;
import java.util.List;

/** Shared in-memory service for payslip disputes during the current app session. */
public final class PayslipDisputeManager {
    private static final PayslipDisputeManager INSTANCE = new PayslipDisputeManager();
    private final List<PayslipDispute> disputes = new ArrayList<>();

    private PayslipDisputeManager() {}

    public static PayslipDisputeManager getInstance() { return INSTANCE; }

    public synchronized void submitDispute(PayslipDispute dispute) {
        if (dispute == null) throw new IllegalArgumentException("Dispute is required.");
        if (findDispute(dispute.getDisputeId()) != null)
            throw new IllegalArgumentException("Dispute ID already exists.");
        disputes.add(dispute);
    }

    public synchronized List<PayslipDispute> getAllDisputes() {
        return new ArrayList<>(disputes);
    }

    public synchronized List<PayslipDispute> getEmployeeDisputes(String employeeId) {
        List<PayslipDispute> result = new ArrayList<>();
        for (PayslipDispute d : disputes)
            if (d.getEmployeeId().equals(employeeId)) result.add(d);
        return result;
    }

    public synchronized PayslipDispute findDispute(String id) {
        for (PayslipDispute d : disputes)
            if (d.getDisputeId().equals(id)) return d;
        return null;
    }

    public synchronized void reviewDispute(String id, String staffId) {
        getRequired(id).markUnderReview(staffId);
    }

    public synchronized void resolveDispute(String id, String staffId, String notes) {
        getRequired(id).resolve(staffId, notes);
    }

    public synchronized void rejectDispute(String id, String staffId, String notes) {
        getRequired(id).reject(staffId, notes);
    }

    private PayslipDispute getRequired(String id) {
        PayslipDispute d = findDispute(id);
        if (d == null) throw new IllegalArgumentException("Dispute not found: " + id);
        return d;
    }
}
