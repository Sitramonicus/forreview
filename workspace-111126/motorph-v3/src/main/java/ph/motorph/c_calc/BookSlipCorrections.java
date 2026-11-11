package ph.motorph.c_calc;

import java.math.BigDecimal;
import java.time.YearMonth;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeImport;

public final class BookSlipCorrections {
    
    public static final YearMonth SAMPLE_MONTH = YearMonth.of(2026, 1);
    
    public static final int SAMPLE_CUTOFF = 0;
    
    private static final int STORED_CUTOFF = SAMPLE_CUTOFF + 1;
    public static final String DESCRIPTION =
        "Phone allowance difference carried from MotorPH's sample payslip (sample 2,000.00 vs employee file 1,000.00)";
    public static final String AUTHOR = "system: sample payslip reconciliation";

    private BookSlipCorrections() {}

    
    public static long book(PayrollAdjustments adjustments, Employee employee) {
        if (adjustments == null || employee == null) return 0;
        if (!EmployeeImport.SAMPLE_EMPLOYEE.equals(employee.getEid())) return 0;
        long difference = EmployeeImport.SAMPLE_PHONE_ALLOWANCE.subtract(employee.getPhoneAllowance()).movePointRight(2).longValue();
        if (difference == 0) return 0;
        for (PayrollAdjustments.Earning existing : adjustments.earningsFor(employee.getEid(), SAMPLE_MONTH, STORED_CUTOFF)) {
            if (DESCRIPTION.equals(existing.description)) return difference;   
        }
        adjustments.addAllowanceCorrection(employee.getEid(), SAMPLE_MONTH, STORED_CUTOFF,
            PayrollAdjustments.ALLOWANCE_TYPES.get(MonthPayroll.PHONE), DESCRIPTION, difference, AUTHOR);
        return difference;
    }

    
    public static long bookForSampleEmployee(PayrollAdjustments adjustments, Iterable<Employee> employees) {
        for (Employee employee : employees) {
            if (EmployeeImport.SAMPLE_EMPLOYEE.equals(employee.getEid())) return book(adjustments, employee);
        }
        return 0;
    }

    
    public static boolean isNeeded(Employee employee) {
        return employee != null && EmployeeImport.SAMPLE_EMPLOYEE.equals(employee.getEid())
            && employee.getPhoneAllowance().compareTo(EmployeeImport.SAMPLE_PHONE_ALLOWANCE) != 0;
    }

    
    public static BigDecimal difference(Employee employee) {
        if (employee == null) return BigDecimal.ZERO;
        return EmployeeImport.SAMPLE_PHONE_ALLOWANCE.subtract(employee.getPhoneAllowance());
    }
}
