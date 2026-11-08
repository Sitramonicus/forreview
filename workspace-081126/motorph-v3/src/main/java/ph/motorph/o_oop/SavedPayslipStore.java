package ph.motorph.o_oop;

import ph.motorph.svc.AppServer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finalized payslip snapshots (JSON text), keyed by payslip ID. A snapshot never changes after it is saved, even if
 * attendance, leave or overtime change later. A wrong, unreleased payslip can be voided (with the System Admin's
 * approval): it moves to the voided archive under "<payslipId>-void<n>" and the cutoff can be finalized again.
 * shortcut: process memory, saved to the local data file by AppServer; Firestore needs published rules.
 */
public final class SavedPayslipStore {
    private final Map<String, String> snapshots = new LinkedHashMap<>();
    private Map<String, String> voided = new LinkedHashMap<>();

    /** Data files from before voiding existed have no archive yet. */
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

    /** The active snapshot, or a voided one by its archive ID. */
    public synchronized Optional<String> find(String payslipId) {
        String active = snapshots.get(payslipId);
        return Optional.ofNullable(active != null ? active : voided().get(payslipId));
    }

    public synchronized boolean isActive(String payslipId) { return snapshots.containsKey(payslipId); }

    /** How many times this payslip was voided, so the next finalized one is version count + 1. */
    public synchronized int voidCount(String payslipId) {
        int count = 0;
        for (String id : voided().keySet()) if (id.startsWith(payslipId + "-void")) count++;
        return count;
    }

    /** Moves the active snapshot to the archive (already marked voided by the caller); returns the archive ID. */
    public synchronized String voidSnapshot(String payslipId, String voidedSnapshotJson) {
        if (!snapshots.containsKey(payslipId)) throw new IllegalStateException("Only an active finalized payslip can be voided.");
        String archiveId = payslipId + "-void" + (voidCount(payslipId) + 1);
        snapshots.remove(payslipId);
        voided().put(archiveId, voidedSnapshotJson);
        return archiveId;
    }

    /** Active snapshots, newest first. */
    public synchronized List<String> list() {
        List<String> result = new ArrayList<>(snapshots.values());
        java.util.Collections.reverse(result);
        return result;
    }

    /** Voided snapshots, newest first. */
    public synchronized List<String> listVoided() {
        List<String> result = new ArrayList<>(voided().values());
        java.util.Collections.reverse(result);
        return result;
    }
}
