package ph.motorph.web;

import java.util.Collections;
import java.util.List;

/** A saved payroll run: header data, the ruleset used, and one payslip per employee. */
public final class PayrollRun {
    private final String id;
    private final String payPeriod;
    private final String periodStart;
    private final String periodEnd;
    private final String rulesetId;
    private final String taxBasis;
    private final String statutoryBasis;
    private final String createdAt;
    private final List<Payslip> payslips;

    public PayrollRun(String id, String payPeriod, String periodStart, String periodEnd, String rulesetId,
                      String taxBasis, String statutoryBasis, String createdAt, List<Payslip> payslips) {
        if (id == null || !id.matches("PR-[A-Z0-9-]{4,40}")) {
            throw new IllegalArgumentException("Payroll run ID is invalid.");
        }
        this.id = id;
        this.payPeriod = payPeriod;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.rulesetId = rulesetId;
        this.taxBasis = taxBasis;
        this.statutoryBasis = statutoryBasis;
        this.createdAt = createdAt;
        this.payslips = Collections.unmodifiableList(payslips);
        if (payslips.isEmpty()) throw new IllegalArgumentException("A payroll run needs at least one payslip.");
    }

    public String id() { return id; }
    public String payPeriod() { return payPeriod; }
    public String periodStart() { return periodStart; }
    public String periodEnd() { return periodEnd; }
    public String rulesetId() { return rulesetId; }
    public String taxBasis() { return taxBasis; }
    public String statutoryBasis() { return statutoryBasis; }
    public String createdAt() { return createdAt; }
    public List<Payslip> payslips() { return payslips; }
}
