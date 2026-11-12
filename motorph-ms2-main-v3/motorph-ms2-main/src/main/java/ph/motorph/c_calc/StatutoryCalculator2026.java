package ph.motorph.c_calc;

import java.time.YearMonth;

public final class StatutoryCalculator2026 {
    public static final String RULESET_ID = "PH-2026-MOTORPH-ESTIMATE-v5";
    private static final long BP = 10_000;
    private static final long MAX_INPUT_CENTS = 1_000_000_000L; 

    
    
    private static final long SSS_STEP = 50_000;
    private static final long SSS_EC_THRESHOLD = 1_500_000, SSS_EC_LOW = 1_000, SSS_EC_HIGH = 3_000;

    private StatutoryCalculator2026() { }

    
    public static long parsePesoCents(String value, String field) {
        String text = value == null ? "" : value.trim().replace(",", "");
        if (!text.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException(field + " must be a non-negative peso amount with up to two decimal places.");
        }
        String[] parts = text.split("\\.");
        String fraction = parts.length > 1 ? (parts[1] + "0").substring(0, 2) : "00";
        long cents = Long.parseLong(parts[0]) * 100 + Long.parseLong(fraction);
        if (cents > MAX_INPUT_CENTS) throw new IllegalArgumentException(field + " must be ₱10,000,000.00 or less.");
        return cents;
    }

    public static StatutoryBreakdown calculate(YearMonth period, long grossCashPayCents, long sssCompensationCents,
            long philHealthBasicSalaryCents, long pagIbigFundSalaryCents, long taxableCompensationCents) {
        return calculate(period, grossCashPayCents, sssCompensationCents, philHealthBasicSalaryCents, pagIbigFundSalaryCents,
            taxableCompensationCents, StatutoryRates.DEFAULT);
    }

    
    public static StatutoryBreakdown calculate(YearMonth period, long grossCashPayCents, long sssCompensationCents,
            long philHealthBasicSalaryCents, long pagIbigFundSalaryCents, long taxableCompensationCents, StatutoryRates rates) {
        if (period == null || period.getYear() != 2026) {
            throw new IllegalArgumentException("This temporary ruleset covers 2026 monthly periods only.");
        }
        for (long cents : new long[] {grossCashPayCents, sssCompensationCents, philHealthBasicSalaryCents,
                pagIbigFundSalaryCents, taxableCompensationCents}) {
            if (cents < 0 || cents > MAX_INPUT_CENTS) throw new IllegalArgumentException("Amounts must be ₱0 to ₱10,000,000.00.");
        }
        if (grossCashPayCents <= 0) throw new IllegalArgumentException("Gross cash pay must be greater than zero for this preview.");
        if (sssCompensationCents <= 0 || philHealthBasicSalaryCents <= 0 || pagIbigFundSalaryCents <= 0) {
            throw new IllegalArgumentException("Enter a positive contribution base for SSS, PhilHealth, and Pag-IBIG.");
        }

        long msc = sssMonthlySalaryCredit(sssCompensationCents, rates);
        long sssEmployee = applyBp(msc, rates.sssEmployeeBp);
        long sssEmployer = applyBp(msc, rates.sssEmployerBp);
        long sssEc = msc < SSS_EC_THRESHOLD ? SSS_EC_LOW : SSS_EC_HIGH;

        long phBase = Math.min(Math.max(philHealthBasicSalaryCents, rates.philHealthFloor), rates.philHealthCeiling);
        long phTotal = applyBp(phBase, rates.philHealthPremiumBp);
        
        long phEmployee = applyBp(phBase, rates.philHealthPremiumBp / 2);

        long pagIbigBase = Math.min(pagIbigFundSalaryCents, rates.pagIbigCap);
        long pagIbigEmployee = applyBp(pagIbigBase, pagIbigFundSalaryCents <= rates.pagIbigLowBand ? rates.pagIbigLowBp : rates.pagIbigHighBp);
        long pagIbigEmployer = applyBp(pagIbigBase, rates.pagIbigEmployerBp);

        long employeeStatutory = sssEmployee + phEmployee + pagIbigEmployee;
        long taxable = Math.max(0, taxableCompensationCents - employeeStatutory);
        long tax = annualisedWithholding(taxable, rates);
        if (employeeStatutory + tax > grossCashPayCents) {
            throw new IllegalArgumentException("Employee deductions exceed gross cash pay; preview blocked rather than showing negative net pay.");
        }
        return new StatutoryBreakdown(rates.id, period, grossCashPayCents, sssCompensationCents,
            philHealthBasicSalaryCents, pagIbigFundSalaryCents, taxableCompensationCents,
            msc, sssEmployee, sssEmployer, sssEc, phBase, phTotal, phEmployee,
            pagIbigBase, pagIbigEmployee, pagIbigEmployer, taxable, tax);
    }

    private static long sssMonthlySalaryCredit(long compensationCents, StatutoryRates rates) {
        long firstHigherRange = rates.sssMinMsc + SSS_STEP / 2; 
        if (compensationCents < firstHigherRange) return rates.sssMinMsc;
        long msc = rates.sssMinMsc + ((compensationCents - firstHigherRange) / SSS_STEP + 1) * SSS_STEP;
        return Math.min(msc, rates.sssMaxMsc);
    }

    
    private static long annualisedWithholding(long monthlyTaxableCents, StatutoryRates rates) {
        long annualTaxable = monthlyTaxableCents * 12;
        long[] bracket = rates.birAnnual.get(0);
        for (long[] candidate : rates.birAnnual) if (annualTaxable > candidate[0]) bracket = candidate;
        
        long annualTaxScaled = bracket[1] * BP + (annualTaxable - bracket[0]) * bracket[2];
        return roundRatio(annualTaxScaled, BP * 12);
    }

    private static long applyBp(long cents, long basisPoints) { return roundRatio(cents * basisPoints, BP); }

    private static long roundRatio(long numerator, long denominator) { return (numerator + denominator / 2) / denominator; }
}
