package ph.motorph.c_calc;

import java.math.BigDecimal;
import java.time.YearMonth;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeImport;

/**
 * The one place where MotorPH's worked-example payslip and the employee file disagree, written down instead of
 * smoothed over.
 *
 * <p>The sample slip for employee 10015 (Romualdez, Fredrick) pays a <b>2,000.00</b> phone allowance; the employee
 * file states <b>1,000.00</b>. Both numbers are true to their own source, so neither is deleted. The file keeps
 * 1,000.00 as the contracted allowance, and the sample's extra 1,000.00 is booked as a named, dated allowance
 * correction on the sample's own pay period. That way:</p>
 *
 * <ul>
 *   <li>the payslip reproduces the sample to the centavo (benefits 4,500.00, take-home 22,711.67);</li>
 *   <li>the correction is an ordinary {@link PayrollAdjustments} record — it has an id, a description, an author and
 *       a cut-off, so it appears in the adjustment list and in the audit trail like any other;</li>
 *   <li>it never touches another employee, another month, or the employee file itself, and it is idempotent, so
 *       restarting the application cannot stack it twice.</li>
 * </ul>
 *
 * <p>If the employee file is ever corrected to 2,000.00 this class books nothing, and the payslip still reconciles —
 * the disclosed difference simply disappears because the disagreement no longer exists.</p>
 */
public final class SampleSlipCorrections {
    /** The sample slip's pay period: 1–15 January 2026. */
    public static final YearMonth SAMPLE_MONTH = YearMonth.of(2026, 1);
    /** The cut-off it belongs to: the first one (1–15), numbered 0 the way {@link MonthPayroll} numbers cut-offs. */
    public static final int SAMPLE_CUTOFF = 0;
    /** The same cut-off numbered the way {@link PayrollAdjustments} stores it (1 = 1–15, 2 = 16–end). */
    private static final int STORED_CUTOFF = SAMPLE_CUTOFF + 1;
    public static final String DESCRIPTION =
        "Phone allowance difference carried from MotorPH's sample payslip (sample 2,000.00 vs employee file 1,000.00)";
    public static final String AUTHOR = "system: sample payslip reconciliation";

    private SampleSlipCorrections() {}

    /**
     * Books the disclosed correction for one employee if the file and the sample still disagree, and returns the
     * amount booked in centavos (0 when nothing was needed). Safe to call on every start-up.
     */
    public static long book(PayrollAdjustments adjustments, Employee employee) {
        if (adjustments == null || employee == null) return 0;
        if (!EmployeeImport.SAMPLE_EMPLOYEE.equals(employee.getEid())) return 0;
        long difference = EmployeeImport.SAMPLE_PHONE_ALLOWANCE.subtract(employee.getPhoneAllowance()).movePointRight(2).longValue();
        if (difference == 0) return 0;
        for (PayrollAdjustments.Earning existing : adjustments.earningsFor(employee.getEid(), SAMPLE_MONTH, STORED_CUTOFF)) {
            if (DESCRIPTION.equals(existing.description)) return difference;   // already booked; never stack it
        }
        adjustments.addAllowanceCorrection(employee.getEid(), SAMPLE_MONTH, STORED_CUTOFF,
            PayrollAdjustments.ALLOWANCE_TYPES.get(MonthPayroll.PHONE), DESCRIPTION, difference, AUTHOR);
        return difference;
    }

    /** Convenience for the demo start-up: books the correction for whichever employee the sample names. */
    public static long bookForSampleEmployee(PayrollAdjustments adjustments, Iterable<Employee> employees) {
        for (Employee employee : employees) {
            if (EmployeeImport.SAMPLE_EMPLOYEE.equals(employee.getEid())) return book(adjustments, employee);
        }
        return 0;
    }

    /** True when the employee file still carries the figure that makes this correction necessary. */
    public static boolean isNeeded(Employee employee) {
        return employee != null && EmployeeImport.SAMPLE_EMPLOYEE.equals(employee.getEid())
            && employee.getPhoneAllowance().compareTo(EmployeeImport.SAMPLE_PHONE_ALLOWANCE) != 0;
    }

    /** The difference in pesos, for messages: 1,000.00. */
    public static BigDecimal difference(Employee employee) {
        if (employee == null) return BigDecimal.ZERO;
        return EmployeeImport.SAMPLE_PHONE_ALLOWANCE.subtract(employee.getPhoneAllowance());
    }
}
