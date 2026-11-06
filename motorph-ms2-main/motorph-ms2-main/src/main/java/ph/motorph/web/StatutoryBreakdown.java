package ph.motorph.web;

import java.time.YearMonth;

/** One month's employee/employer statutory amounts, all in centavos. Not a payslip. */
public final class StatutoryBreakdown {
    public final String rulesetId;
    public final YearMonth period;
    public final long grossCashPayCents;
    public final long sssCompensationCents;
    public final long philHealthBasicSalaryCents;
    public final long pagIbigFundSalaryCents;
    public final long taxableCompensationCents;
    public final long sssMscCents;
    public final long sssEmployeeCents;
    public final long sssEmployerRegularCents;
    public final long sssEcCents;
    public final long philHealthPremiumBaseCents;
    public final long philHealthTotalPremiumCents;
    public final long philHealthEmployeeCents;
    public final long philHealthEmployerCents;
    public final long pagIbigBaseCents;
    public final long pagIbigEmployeeCents;
    public final long pagIbigEmployerCents;
    public final long taxableAfterEmployeeContributionsCents;
    public final long withholdingTaxCents;

    StatutoryBreakdown(String rulesetId, YearMonth period, long grossCashPayCents, long sssCompensationCents,
            long philHealthBasicSalaryCents, long pagIbigFundSalaryCents, long taxableCompensationCents,
            long sssMscCents, long sssEmployeeCents, long sssEmployerRegularCents, long sssEcCents,
            long philHealthPremiumBaseCents, long philHealthTotalPremiumCents, long philHealthEmployeeCents,
            long pagIbigBaseCents, long pagIbigEmployeeCents, long pagIbigEmployerCents,
            long taxableAfterEmployeeContributionsCents, long withholdingTaxCents) {
        this.rulesetId = rulesetId;
        this.period = period;
        this.grossCashPayCents = grossCashPayCents;
        this.sssCompensationCents = sssCompensationCents;
        this.philHealthBasicSalaryCents = philHealthBasicSalaryCents;
        this.pagIbigFundSalaryCents = pagIbigFundSalaryCents;
        this.taxableCompensationCents = taxableCompensationCents;
        this.sssMscCents = sssMscCents;
        this.sssEmployeeCents = sssEmployeeCents;
        this.sssEmployerRegularCents = sssEmployerRegularCents;
        this.sssEcCents = sssEcCents;
        this.philHealthPremiumBaseCents = philHealthPremiumBaseCents;
        this.philHealthTotalPremiumCents = philHealthTotalPremiumCents;
        this.philHealthEmployeeCents = philHealthEmployeeCents;
        this.philHealthEmployerCents = philHealthTotalPremiumCents - philHealthEmployeeCents;
        this.pagIbigBaseCents = pagIbigBaseCents;
        this.pagIbigEmployeeCents = pagIbigEmployeeCents;
        this.pagIbigEmployerCents = pagIbigEmployerCents;
        this.taxableAfterEmployeeContributionsCents = taxableAfterEmployeeContributionsCents;
        this.withholdingTaxCents = withholdingTaxCents;
    }

    public long sssEmployerTotalCents() { return sssEmployerRegularCents + sssEcCents; }
    public long totalEmployeeStatutoryCents() { return sssEmployeeCents + philHealthEmployeeCents + pagIbigEmployeeCents; }
    public long totalEmployeeDeductionsCents() { return totalEmployeeStatutoryCents() + withholdingTaxCents; }
    public long totalEmployerContributionsCents() { return sssEmployerTotalCents() + philHealthEmployerCents + pagIbigEmployerCents; }
    public long netPayCents() { return grossCashPayCents - totalEmployeeDeductionsCents(); }
    public long employerCashCostCents() { return grossCashPayCents + totalEmployerContributionsCents(); }
}
