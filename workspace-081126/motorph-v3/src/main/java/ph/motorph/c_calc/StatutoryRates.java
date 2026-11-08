package ph.motorph.c_calc;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One version of the statutory rates and tax table (matrix: Tax Table/Rates, System Admin C/R/U/D).
 * Money in centavos, rates in basis points (1% = 100). DEFAULT is the sweldongpinoy.com-matched 2026 set; the admin can
 * add versions effective from a 2026 month, and payroll uses the latest version on or before each month.
 * Fixed (not editable here): SSS MSC step ₱500 and the EC amounts (₱10 below ₱15,000 MSC, ₱30 from it).
 */
public final class StatutoryRates {
    public final String id, note, createdBy, createdAt;
    public final YearMonth effectiveMonth;
    public final long sssEmployeeBp, sssEmployerBp, sssMinMsc, sssMaxMsc;
    public final long philHealthPremiumBp, philHealthFloor, philHealthCeiling;
    public final long pagIbigLowBand, pagIbigLowBp, pagIbigHighBp, pagIbigEmployerBp, pagIbigCap;
    /** {over, base tax} in centavos and the marginal rate in basis points, ascending, first row over 0. */
    public final List<long[]> birAnnual;

    public static final StatutoryRates DEFAULT = new StatutoryRates(StatutoryCalculator2026.RULESET_ID, YearMonth.of(2026, 1),
        "Built-in 2026 rates (matches sweldongpinoy.com; Pag-IBIG 1% at exactly ₱1,500 per Circular 460)", "", "",
        500, 1_000, 500_000, 3_500_000, 500, 1_000_000, 10_000_000, 150_000, 100, 200, 200, 1_000_000,
        java.util.Arrays.asList(new long[] {0, 0, 0}, new long[] {25_000_000, 0, 1_500}, new long[] {40_000_000, 2_250_000, 2_000},
            new long[] {80_000_000, 10_250_000, 2_500}, new long[] {200_000_000, 40_250_000, 3_000}, new long[] {800_000_000, 220_250_000, 3_500}));

    StatutoryRates(String id, YearMonth effectiveMonth, String note, String createdBy, String createdAt,
            long sssEmployeeBp, long sssEmployerBp, long sssMinMsc, long sssMaxMsc,
            long philHealthPremiumBp, long philHealthFloor, long philHealthCeiling,
            long pagIbigLowBand, long pagIbigLowBp, long pagIbigHighBp, long pagIbigEmployerBp, long pagIbigCap, List<long[]> birAnnual) {
        this.id = id; this.effectiveMonth = effectiveMonth; this.note = note; this.createdBy = createdBy; this.createdAt = createdAt;
        this.sssEmployeeBp = sssEmployeeBp; this.sssEmployerBp = sssEmployerBp; this.sssMinMsc = sssMinMsc; this.sssMaxMsc = sssMaxMsc;
        this.philHealthPremiumBp = philHealthPremiumBp; this.philHealthFloor = philHealthFloor; this.philHealthCeiling = philHealthCeiling;
        this.pagIbigLowBand = pagIbigLowBand; this.pagIbigLowBp = pagIbigLowBp; this.pagIbigHighBp = pagIbigHighBp;
        this.pagIbigEmployerBp = pagIbigEmployerBp; this.pagIbigCap = pagIbigCap;
        List<long[]> rows = new ArrayList<>();
        for (long[] row : birAnnual) rows.add(row.clone());
        this.birAnnual = Collections.unmodifiableList(rows);
        validate();
    }

    private void validate() {
        for (long bp : new long[] {sssEmployeeBp, sssEmployerBp, philHealthPremiumBp, pagIbigLowBp, pagIbigHighBp, pagIbigEmployerBp}) {
            if (bp < 0 || bp > 5_000) throw new IllegalArgumentException("Contribution rates must be 0% to 50%.");
        }
        if (philHealthPremiumBp % 2 != 0) throw new IllegalArgumentException("The PhilHealth premium must split evenly (e.g. 5.00%).");
        if (sssMinMsc <= 0 || sssMaxMsc < sssMinMsc || (sssMaxMsc - sssMinMsc) % 50_000 != 0) {
            throw new IllegalArgumentException("SSS MSC range must be positive, with the maximum at least the minimum, in ₱500 steps.");
        }
        if (philHealthFloor <= 0 || philHealthCeiling < philHealthFloor) throw new IllegalArgumentException("PhilHealth floor and ceiling must be positive, ceiling at least floor.");
        if (pagIbigLowBand < 0 || pagIbigCap <= 0) throw new IllegalArgumentException("Pag-IBIG band and cap must be positive.");
        if (birAnnual.isEmpty() || birAnnual.size() > 12 || birAnnual.get(0)[0] != 0) throw new IllegalArgumentException("The tax table needs 1-12 rows, the first starting at ₱0.");
        for (int i = 0; i < birAnnual.size(); i++) {
            long[] row = birAnnual.get(i);
            if (row.length != 3 || row[1] < 0 || row[2] < 0 || row[2] > 10_000) throw new IllegalArgumentException("Each tax row needs over ≥ 0, base tax ≥ 0 and a rate of 0% to 100%.");
            if (i > 0 && row[0] <= birAnnual.get(i - 1)[0]) throw new IllegalArgumentException("Tax table rows must go up in income.");
        }
    }

    /** A new admin version with these numbers. */
    public static StatutoryRates version(String id, YearMonth effectiveMonth, String note, String createdBy,
            long sssEmployeeBp, long sssEmployerBp, long sssMinMsc, long sssMaxMsc,
            long philHealthPremiumBp, long philHealthFloor, long philHealthCeiling,
            long pagIbigLowBand, long pagIbigLowBp, long pagIbigHighBp, long pagIbigEmployerBp, long pagIbigCap, List<long[]> birAnnual) {
        if (effectiveMonth == null || effectiveMonth.getYear() != 2026) throw new IllegalArgumentException("Effective month must be in 2026.");
        return new StatutoryRates(id, effectiveMonth, note == null ? "" : note.trim(), createdBy, Instant.now().toString(),
            sssEmployeeBp, sssEmployerBp, sssMinMsc, sssMaxMsc, philHealthPremiumBp, philHealthFloor, philHealthCeiling,
            pagIbigLowBand, pagIbigLowBp, pagIbigHighBp, pagIbigEmployerBp, pagIbigCap, birAnnual);
    }

    /** Versions by effective month; payroll uses the latest on or before the month. Local demo, process memory. */
    public static final class Registry {
        private final List<StatutoryRates> versions = new ArrayList<>();

        public synchronized StatutoryRates ratesFor(YearMonth month) {
            StatutoryRates chosen = DEFAULT;
            for (StatutoryRates version : versions) {
                if (!version.effectiveMonth.isAfter(month) && !version.effectiveMonth.isBefore(chosen.effectiveMonth)) chosen = version;
            }
            return chosen;
        }

        public synchronized String nextId() { return "PH-2026-admin-v" + (versions.size() + 1); }

        public synchronized StatutoryRates add(StatutoryRates version) {
            for (StatutoryRates existing : versions) {
                if (existing.effectiveMonth.equals(version.effectiveMonth)) {
                    throw new IllegalStateException("A rates version already starts in " + version.effectiveMonth + "; choose another month.");
                }
            }
            versions.add(version);
            return version;
        }

        /** The built-in default first, then admin versions in the order added. */
        public synchronized List<StatutoryRates> all() {
            List<StatutoryRates> result = new ArrayList<>();
            result.add(DEFAULT);
            result.addAll(versions);
            return result;
        }
    }
}
