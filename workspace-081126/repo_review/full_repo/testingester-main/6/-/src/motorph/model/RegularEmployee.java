package motorph.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A regularized employee. Receives rice subsidy, phone allowance, and clothing
 * allowance as a regularization benefit.
 */
public class RegularEmployee extends Employee {

    public RegularEmployee(String employeeId, String lastName, String firstName, LocalDate birthday,
                           String address, String phoneNumber, String sssNumber, String philHealthNumber,
                           String tin, String pagIbigNumber, String position, String immediateSupervisor,
                           BigDecimal basicSalary, BigDecimal riceSubsidy, BigDecimal phoneAllowance,
                           BigDecimal clothingAllowance, BigDecimal grossSemiMonthlyRate, BigDecimal hourlyRate) {
        super(employeeId, lastName, firstName, birthday, address, phoneNumber, sssNumber, philHealthNumber,
              tin, pagIbigNumber, STATUS_REGULAR, position, immediateSupervisor, basicSalary, riceSubsidy,
              phoneAllowance, clothingAllowance, grossSemiMonthlyRate, hourlyRate);
    }

    /** Returns rice subsidy + phone allowance + clothing allowance. */
    @Override
    public BigDecimal computeAllowances() {
        return getRiceSubsidy().add(getPhoneAllowance()).add(getClothingAllowance());
    }

    @Override
    public String getEmploymentType() {
        return STATUS_REGULAR;
    }
}
