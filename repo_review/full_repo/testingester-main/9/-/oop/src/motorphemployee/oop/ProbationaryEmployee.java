package motorphemployee.oop;

/**
 * ProbationaryEmployee - concrete subclass for employees on probation (MS1 Week 3/4/5 CRC rows).
 *
 * Same contract as RegularEmployee, different values. Nothing is duplicated from the parents.
 */
public class ProbationaryEmployee extends Employee {

    private int probationMonths;
    private String startDate;
    private double evaluationScore;

    /** Same 19 fields as Employee + probationMonths, startDate, evaluationScore (MS1 Week 4 CRC). */
    public ProbationaryEmployee(int id,
                                String lastName,
                                String firstName,
                                String birthday,
                                String address,
                                String phoneNumber,
                                String sssNumber,
                                String philhealthNumber,
                                String tinNumber,
                                String pagIbigNumber,
                                String status,
                                String position,
                                String immediateSupervisor,
                                double basicSalary,
                                double riceSubsidy,
                                double phoneAllowance,
                                double clothingAllowance,
                                double grossSemiMonthlyRate,
                                double hourlyRate,
                                int probationMonths,
                                String startDate,
                                double evaluationScore) {
        super(id, lastName, firstName, birthday, address, phoneNumber, sssNumber, philhealthNumber,
                tinNumber, pagIbigNumber, status, position, immediateSupervisor, basicSalary,
                riceSubsidy, phoneAllowance, clothingAllowance, grossSemiMonthlyRate, hourlyRate);
        this.probationMonths = requireProbationMonths(probationMonths);
        this.startDate = requireStartDate(startDate);
        this.evaluationScore = requireEvaluationScore(evaluationScore);
    }

    public int getProbationMonths() {
        return probationMonths;
    }

    public void setProbationMonths(int probationMonths) {
        this.probationMonths = requireProbationMonths(probationMonths);
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = requireStartDate(startDate);
    }

    public double getEvaluationScore() {
        return evaluationScore;
    }

    public void setEvaluationScore(double evaluationScore) {
        this.evaluationScore = requireEvaluationScore(evaluationScore);
    }

    private static int requireProbationMonths(int probationMonths) {
        if (probationMonths < 0 || probationMonths > 12) {
            throw new IllegalArgumentException("probationMonths out of range: " + probationMonths);
        }
        return probationMonths;
    }

    private static String requireStartDate(String startDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            throw new IllegalArgumentException("startDate cannot be null or empty");
        }
        return startDate.trim();
    }

    private static double requireEvaluationScore(double evaluationScore) {
        if (evaluationScore < 0 || evaluationScore > 100) {
            throw new IllegalArgumentException("evaluationScore must be 0-100: " + evaluationScore);
        }
        return evaluationScore;
    }

    /** Reduced rice subsidy during probation. */
    @Override
    public double getRiceSubsidy() {
        return 1000.0;
    }

    /** Reduced phone allowance during probation. */
    @Override
    public double getPhoneAllowance() {
        return 500.0;
    }

    /** Reduced annual leave credits. */
    @Override
    public int getAnnualLeaveCredits() {
        return 6;
    }

    /** Not promotable while on probation. */
    @Override
    public boolean isEligibleForPromotion() {
        return false;
    }
}
