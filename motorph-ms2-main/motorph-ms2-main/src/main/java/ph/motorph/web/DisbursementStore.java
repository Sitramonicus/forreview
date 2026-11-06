package ph.motorph.web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Released pay (local demo, process memory): a batch issues the net pay of a cutoff's finalized payslips by direct
 * deposit or check. A payslip can be in only one batch. Nothing is sent to a bank; this is the payroll record.
 */
public final class DisbursementStore {
    public static final class Line {
        public final String payslipId, employeeId, employeeName, method;
        public final long netCents;

        public Line(String payslipId, String employeeId, String employeeName, String method, long netCents) {
            this.payslipId = payslipId; this.employeeId = employeeId; this.employeeName = employeeName; this.method = method; this.netCents = netCents;
        }
    }

    public static final class Batch {
        public final String id, month, releasedBy, releasedAt;
        public final int cutoff;
        public final List<Line> lines;
        public final long totalCents;

        Batch(String id, String month, int cutoff, List<Line> lines, String releasedBy) {
            this.id = id; this.month = month; this.cutoff = cutoff; this.releasedBy = releasedBy;
            this.releasedAt = Instant.now().toString();
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            long total = 0;
            for (Line line : lines) total += line.netCents;
            this.totalCents = total;
        }
    }

    private final List<Batch> batches = new ArrayList<>();
    private final Set<String> released = new HashSet<>();

    /** Only payslips not already in a batch; none left is a conflict. */
    public synchronized Batch release(String month, int cutoff, List<Line> candidates, String releasedBy) {
        List<Line> lines = new ArrayList<>();
        for (Line line : candidates) if (!released.contains(line.payslipId)) lines.add(line);
        if (lines.isEmpty()) throw new IllegalStateException("No finalized payslips are waiting for release in this cutoff.");
        Batch batch = new Batch("DB-" + month + "-" + cutoff + "-" + (batches.size() + 1), month, cutoff, lines, releasedBy);
        batches.add(batch);
        for (Line line : lines) released.add(line.payslipId);
        return batch;
    }

    public synchronized boolean isReleased(String payslipId) { return released.contains(payslipId); }

    /** Newest first. */
    public synchronized List<Batch> list() {
        List<Batch> result = new ArrayList<>(batches);
        Collections.reverse(result);
        return result;
    }
}
