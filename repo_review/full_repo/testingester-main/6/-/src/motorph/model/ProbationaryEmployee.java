package motorph.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An employee who has not been regularized yet.
 * <p>
 * Project assumption (MS1 design, kept after review): allowances begin after
 * regularization, so this type returns 0 allowances. employees.csv does list
 * allowance amounts for probationary staff; CP2 never paid allowances to any
 * employee, so this rule changes no payroll totals.
 * </p>
 */
public class ProbationaryEmployee extends Employee {

    public ProbationaryEmployee(String employeeId, String lastName, String firstName, LocalDate birthday,
                                String address, String phoneNumber, String sssNumber, String philHealthNumber,
                                String tin, String pagIbigNumber, String position, String immediateSupervisor,
                                BigDecimal basicSalary, BigDecimal riceSubsidy, BigDecimal phoneAllowance,
                                BigDecimal clothingAllowance, BigDecimal grossSemiMonthlyRate, BigDecimal hourlyRate) {
        super(employeeId, lastName, firstName, birthday, address, phoneNumber, sssNumber, philHealthNumber,
              tin, pagIbigNumber, STATUS_PROBATIONARY, position, immediateSupervisor, basicSalary, riceSubsidy,
              phoneAllowance, clothingAllowance, grossSemiMonthlyRate, hourlyRate);
    }

    /** Returns 0: no allowances until regularized. */
    @Override
    public BigDecimal computeAllowances() {
        return BigDecimal.ZERO;
    }

    @Override
    public String getEmploymentType() {
        return STATUS_PROBATIONARY;
    }
}
