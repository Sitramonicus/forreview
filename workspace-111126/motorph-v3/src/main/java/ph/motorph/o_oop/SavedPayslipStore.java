package ph.motorph.o_oop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SavedPayslipStore {
    private final Map<String, String> snapshots = new LinkedHashMap<>();
    private Map<String, String> voided = new LinkedHashMap<>();

    
    private Map<String, String> voided() {
        if (voided == null) voided = new LinkedHashMap<>();
        return voided;
    }

    public synchronized void save(String payslipId, String snapshotJson) {
        if (snapshots.containsKey(payslipId)) {
            throw new IllegalStateException("Payslip " + payslipId + " is already finalized; saved payslips cannot be changed.");
        }
        snapshots.put(payslipId, snapshotJson);
    }

    
    public synchronized Optional<String> find(String payslipId) {
        String active = snapshots.get(payslipId);
        return Optional.ofNullable(active != null ? active : voided().get(payslipId));
    }

    public synchronized boolean isActive(String payslipId) { return snapshots.containsKey(payslipId); }

    
    public synchronized int voidCount(String payslipId) {
        int count = 0;
        for (String id : voided().keySet()) if (id.startsWith(payslipId + "-void")) count++;
        return count;
    }

    
    public synchronized String voidSnapshot(String payslipId, String voidedSnapshotJson) {
        if (!snapshots.containsKey(payslipId)) throw new IllegalStateException("Only an active finalized payslip can be voided.");
        String archiveId = payslipId + "-void" + (voidCount(payslipId) + 1);
        snapshots.remove(payslipId);
        voided().put(archiveId, voidedSnapshotJson);
        return archiveId;
    }

    
    public synchronized List<String> list() {
        List<String> result = new ArrayList<>(snapshots.values());
        java.util.Collections.reverse(result);
        return result;
    }

    
    public synchronized List<String> listVoided() {
        List<String> result = new ArrayList<>(voided().values());
        java.util.Collections.reverse(result);
        return result;
    }
}
