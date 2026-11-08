package ph.motorph.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Effective-dated pay rates, so a rate change stops rewriting history.
 *
 * <p>A payslip must be reproducible months later. That is only possible if the rate in force on the
 * period being paid is known, which means storing every rate with the date it took effect rather
 * than overwriting one current value.
 *
 * <pre>
 * SalaryRate(employee 10015, effective 2026-01-01, basic 53,500.00, rice 1,500.00, ...)
 * SalaryRate(employee 10015, effective 2026-07-01, basic 58,000.00, rice 1,500.00, ...)
 *
 * rateOn(10015, 2026-01-15) -> 53,500.00   (the January payslip still reads 53,500)
 * rateOn(10015, 2026-08-01) -> 58,000.00
 * </pre>
 *
 * <p>The rate is looked up by the <b>period end date</b>, so a raise that takes effect mid-cycle
 * applies from the next cycle, which is how a real payroll works.
 */
public final class SalaryHistory {

    /** One rate row: what this employee was paid from {@code effectiveFrom} until the next row. */
    public static final class SalaryRate {
        private final String employeeId;
        private final LocalDate effectiveFrom;
        private final BigDecimal monthlyBasic;
        private final BigDecimal riceSubsidy;
        private final BigDecimal phoneAllowance;
        private final BigDecimal clothingAllowance;
        private final String reason;
        private final String recordedAt;

        public SalaryRate(String employeeId, LocalDate effectiveFrom, BigDecimal monthlyBasic,
                          BigDecimal riceSubsidy, BigDecimal phoneAllowance,
                          BigDecimal clothingAllowance, String reason, String recordedAt) {
            if (!Employee.isValidEmployeeNumber(employeeId)) {
                throw new IllegalArgumentException("Salary history needs a five-digit employee number.");
            }
            if (effectiveFrom == null) {
                throw new IllegalArgumentException("A rate change needs the date it takes effect.");
            }
            this.employeeId = employeeId;
            this.effectiveFrom = effectiveFrom;
            this.monthlyBasic = nonNegative(monthlyBasic, "Monthly basic pay");
            this.riceSubsidy = nonNegative(riceSubsidy, "Rice subsidy");
            this.phoneAllowance = nonNegative(phoneAllowance, "Phone allowance");
            this.clothingAllowance = nonNegative(clothingAllowance, "Clothing allowance");
            this.reason = reason == null ? "" : reason.trim();
            this.recordedAt = recordedAt == null ? "" : recordedAt;
        }

        public String employeeId() { return employeeId; }
        public LocalDate effectiveFrom() { return effectiveFrom; }
        public BigDecimal monthlyBasic() { return monthlyBasic; }
        public BigDecimal riceSubsidy() { return riceSubsidy; }
        public BigDecimal phoneAllowance() { return phoneAllowance; }
        public BigDecimal clothingAllowance() { return clothingAllowance; }
        public String reason() { return reason; }
        public String recordedAt() { return recordedAt; }

        /** The same four amounts, as an Employee-style pay profile. */
        public BigDecimal[] amounts() {
            return new BigDecimal[]{monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance};
        }

        private static BigDecimal nonNegative(BigDecimal value, String field) {
            if (value == null) return BigDecimal.ZERO;
            if (value.signum() < 0) throw new IllegalArgumentException(field + " must not be negative.");
            return value;
        }
    }

    private final List<SalaryRate> rates = new ArrayList<>();

    /** Adds a rate row. Adding the same employee and date twice is refused, not silently merged. */
    /**
     * Replaces the row for an employee and effective date without the duplicate check. Only for
     * seeding, where the row being replaced is one seeding itself just wrote in the same pass.
     */
    public void replaceUnchecked(SalaryRate rate) {
        java.util.Iterator<SalaryRate> it = rates.iterator();
        while (it.hasNext()) {
            SalaryRate existing = it.next();
            if (existing.employeeId().equals(rate.employeeId())
                && existing.effectiveFrom().equals(rate.effectiveFrom())) {
                it.remove();
            }
        }
        add(rate);
    }

    public void add(SalaryRate rate) {
        if (rate == null) throw new IllegalArgumentException("A rate row is required.");
        for (SalaryRate existing : rates) {
            if (existing.employeeId.equals(rate.employeeId) && existing.effectiveFrom.equals(rate.effectiveFrom)) {
                throw new IllegalArgumentException("There is already a rate for " + rate.employeeId
                    + " effective " + rate.effectiveFrom + ".");
            }
        }
        rates.add(rate);
        rates.sort(Comparator.comparing((SalaryRate r) -> r.employeeId)
            .thenComparing(r -> r.effectiveFrom));
    }

    /**
     * The rate in force for a period ending on {@code asOf}. Empty when the employee has no rate
     * that took effect on or before that date — which is a long-service employee predating the
     * history, and must be reported rather than silently filled with today's rate.
     */
    public Optional<SalaryRate> rateOn(String employeeId, LocalDate asOf) {
        SalaryRate found = null;
        for (SalaryRate rate : rates) {
            if (rate.employeeId.equals(employeeId) && !rate.effectiveFrom.isAfter(asOf)) {
                if (found == null || rate.effectiveFrom.isAfter(found.effectiveFrom)) found = rate;
            }
        }
        return Optional.ofNullable(found);
    }

    /** Every rate row for one employee, oldest first. */
    public List<SalaryRate> historyOf(String employeeId) {
        List<SalaryRate> rows = new ArrayList<>();
        for (SalaryRate rate : rates) {
            if (rate.employeeId.equals(employeeId)) rows.add(rate);
        }
        rows.sort(Comparator.comparing(r -> r.effectiveFrom));
        return rows;
    }

    /**
     * The employee record as it was paid for a given period: the identity and placement from the
     * directory, the pay amounts from the rate in force. Returns the directory record unchanged
     * when no rate row exists, so behaviour is never silently different from before.
     */
    public Employee asOf(Employee employee, LocalDate periodEnd) {
        Optional<SalaryRate> rate = rateOn(employee.getId(), periodEnd);
        if (!rate.isPresent()) return employee;
        SalaryRate r = rate.get();
        return employee.withPay(r.monthlyBasic, r.riceSubsidy, r.phoneAllowance, r.clothingAllowance);
    }

    /** True when this employee has at least one dated rate. */
    public boolean hasHistory(String employeeId) {
        return !historyOf(employeeId).isEmpty();
    }

    public List<SalaryRate> all() {
        return new ArrayList<>(rates);
    }
}
