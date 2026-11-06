package ph.motorph.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Approved salary changes (promotions, merit increases) by effective month, over the placeholder basics. Local demo. */
public final class SalaryHistory {
    private final Map<String, TreeMap<YearMonth, BigDecimal>> changes = new HashMap<>();

    public synchronized void set(String employeeId, YearMonth effective, BigDecimal monthlyBasic) {
        changes.computeIfAbsent(employeeId, key -> new TreeMap<>()).put(effective, monthlyBasic.setScale(2, RoundingMode.HALF_UP));
    }

    /** The basic in force for a month: the latest change on or before it, else the profile's placeholder. */
    public synchronized Optional<BigDecimal> monthlyBasic(String employeeId, YearMonth month) {
        TreeMap<YearMonth, BigDecimal> history = changes.get(employeeId);
        if (history != null) {
            Map.Entry<YearMonth, BigDecimal> entry = history.floorEntry(month);
            if (entry != null) return Optional.of(entry.getValue());
        }
        return PayrollProfile.monthlyBasic(employeeId);
    }

    public Optional<BigDecimal> hourlyRate(String employeeId, YearMonth month) {
        return monthlyBasic(employeeId, month).map(basic -> basic.divide(PayrollPolicy.hoursPerMonth(), 2, RoundingMode.HALF_UP));
    }
}
