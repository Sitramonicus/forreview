package ph.motorph.c_calc;

import ph.motorph.o_oop.Employee;
import java.time.YearMonth;

/**
 * 2026 SSS, PhilHealth, Pag-IBIG and withholding tax, matched to sweldongpinoy.com (the mentor-specified
 * reference; 25 sample salaries agree to the centavo). Money is whole centavos (long); rates are basis points
 * (1% = 100), rounded half-up once per line. Not an approved payroll policy.
 */
public final class StatutoryCalculator2026 {
    public static final String RULESET_ID = "PH-2026-sweldongpinoy-v4";
    private static final long BP = 10_000;
    private static final long MAX_INPUT_CENTS = 1_000_000_000L; // ₱10,000,000 keeps every product inside a long

    // The numbers live in StatutoryRates (DEFAULT = SSS Circular 2024-006, PhilHealth PA2025-0002, Pag-IBIG Circular 460
    // with 1% at or below ₱1,500, BIR RR 11-2018 annual table); the System Admin can add versions by effective month.
    private static final long SSS_STEP = 50_000;
    private static final long SSS_EC_THRESHOLD = 1_500_000, SSS_EC_LOW = 1_000, SSS_EC_HIGH = 3_000;

    private StatutoryCalculator2026() { }

    /** Parses "1,234.56"-style non-negative pesos into centavos, rejecting more than two decimals. */
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

    /** Same, with a specific rates version (its id becomes the breakdown's ruleset ID). */
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
        // Employee share rounded from base x half the premium (e.g. 20,833.33 x 2.5% -> 520.83), as sweldongpinoy.com does.
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
        long firstHigherRange = rates.sssMinMsc + SSS_STEP / 2; // e.g. 5,000 MSC covers pay below 5,250
        if (compensationCents < firstHigherRange) return rates.sssMinMsc;
        long msc = rates.sssMinMsc + ((compensationCents - firstHigherRange) / SSS_STEP + 1) * SSS_STEP;
        return Math.min(msc, rates.sssMaxMsc);
    }

    /**
     * 12 x monthly taxable, annual table, then / 12. No 13th month is added (mentor, 2026-11-05): its ₱90,000
     * exemption is annual and cumulative, so it cannot be decided inside one semi-monthly run.
     */
    private static long annualisedWithholding(long monthlyTaxableCents, StatutoryRates rates) {
        long annualTaxable = monthlyTaxableCents * 12;
        long[] bracket = rates.birAnnual.get(0);
        for (long[] candidate : rates.birAnnual) if (annualTaxable > candidate[0]) bracket = candidate;
        // Keep the annual tax exact (centavos x basis points) and round once after dividing by 12.
        long annualTaxScaled = bracket[1] * BP + (annualTaxable - bracket[0]) * bracket[2];
        return roundRatio(annualTaxScaled, BP * 12);
    }

    private static long applyBp(long cents, long basisPoints) { return roundRatio(cents * basisPoints, BP); }

    private static long roundRatio(long numerator, long denominator) { return (numerator + denominator / 2) / denominator; }
}
