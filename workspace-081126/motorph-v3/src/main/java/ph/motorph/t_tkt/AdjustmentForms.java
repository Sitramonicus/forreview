package ph.motorph.t_tkt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Design doc, Payroll Supervision: an adjustment form changes pay components for one cutoff, for an individual, a team,
 * a branch, a department or everyone (Add / Subtract / Replace), waits as Pending, and on approval is applied with a
 * snapshot of each affected employee's figures before and after.
 * Components: the three non-taxable allowances (Add, Subtract, Replace), a taxable earning (Add, Subtract) and a
 * one-cutoff deduction (Add). Withholding-tax rate changes go through the rate-change approval (RateProposals).
 */
public final class AdjustmentForms {
    public static final List<String> ALLOWANCES = Collections.unmodifiableList(Arrays.asList("Rice subsidy", "Phone allowance", "Clothing allowance"));
    public static final String TAXABLE = "Taxable earning", DEDUCTION = "Deduction";

    public static final class Line {
        public final String component, operation;
        public final long amountCents;

        public Line(String component, String operation, long amountCents) {
            this.component = component;
            this.operation = operation;
            this.amountCents = amountCents;
        }
    }

    /** One affected employee: figures for the cutoff before and after, or why they were skipped. */
    public static final class Result {
        public final String employeeId, skipped;
        public final long grossBefore, grossAfter, allowancesBefore, allowancesAfter, deductionsBefore, deductionsAfter, netBefore, netAfter;

        public Result(String employeeId, String skipped, long[] before, long[] after) {
            this.employeeId = employeeId;
            this.skipped = skipped == null ? "" : skipped;
            this.grossBefore = before[0];
            this.allowancesBefore = before[1];
            this.deductionsBefore = before[2];
            this.netBefore = before[3];
            this.grossAfter = after[0];
            this.allowancesAfter = after[1];
            this.deductionsAfter = after[2];
            this.netAfter = after[3];
        }
    }

    public static final class Form {
        public final String id, scope, month, reason, submittedBy, submittedAt;
        public final int cutoff;
        public final List<Line> lines;
        public String status = "Pending review", decidedBy = "", decidedAt = "", outcome = "";
        public List<Result> results = new ArrayList<>();

        Form(String id, String scope, String month, int cutoff, String reason, List<Line> lines, String submittedBy, String submittedAt) {
            this.id = id;
            this.scope = scope;
            this.month = month;
            this.cutoff = cutoff;
            this.reason = reason;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.submittedBy = submittedBy;
            this.submittedAt = submittedAt;
        }
    }

    private final List<Form> forms = new ArrayList<>();
    private int next = 1;

    /** Which operations each component allows (design doc: Add | Subtract | Replace, "non-overlapping fields"). */
    static void checkLine(Line line) {
        boolean allowance = ALLOWANCES.contains(line.component);
        if (!allowance && !TAXABLE.equals(line.component) && !DEDUCTION.equals(line.component)) {
            throw new IllegalArgumentException("Component must be Rice subsidy, Phone allowance, Clothing allowance, Taxable earning or Deduction.");
        }
        List<String> allowed = allowance ? Arrays.asList("Add", "Subtract", "Replace") : TAXABLE.equals(line.component)
            ? Arrays.asList("Add", "Subtract") : Collections.singletonList("Add");
        if (!allowed.contains(line.operation)) throw new IllegalArgumentException(line.component + " allows " + String.join(", ", allowed) + ".");
        if (line.amountCents < 0 || line.amountCents > 100_000_000 || (line.amountCents == 0 && !"Replace".equals(line.operation))) {
            throw new IllegalArgumentException("Amounts must be ₱0.01 to ₱1,000,000 (₱0 only to replace an allowance with nothing).");
        }
    }

    public synchronized Form add(String scope, String month, int cutoff, String reason, List<Line> lines, String by, String at) {
        if (lines.isEmpty() || lines.size() > 5) throw new IllegalArgumentException("A form has 1 to 5 lines.");
        java.util.Set<String> components = new java.util.HashSet<>();
        for (Line line : lines) {
            checkLine(line);
            if (!components.add(line.component)) throw new IllegalArgumentException("Each component appears once per form (" + line.component + ").");
        }
        String text = reason == null ? "" : reason.trim();
        if (text.isEmpty() || text.length() > 200) throw new IllegalArgumentException("A reason (up to 200 characters) is required.");
        Form form = new Form(String.format("AF-%04d", next++), scope, month, cutoff, text, lines, by, at);
        forms.add(form);
        return form;
    }

    public synchronized Form find(String id) {
        for (Form form : forms) if (form.id.equals(id)) return form;
        throw new java.util.NoSuchElementException("Adjustment form not found.");
    }

    public synchronized void decide(Form form, String status, String by, String at, String outcome, List<Result> results) {
        if (!"Pending review".equals(form.status)) throw new IllegalStateException("This form was already " + form.status.toLowerCase() + ".");
        form.status = status;
        form.decidedBy = by;
        form.decidedAt = at;
        form.outcome = outcome;
        form.results = new ArrayList<>(results);
    }

    /** Newest first. */
    public synchronized List<Form> list() {
        List<Form> copy = new ArrayList<>(forms);
        Collections.reverse(copy);
        return copy;
    }
}
