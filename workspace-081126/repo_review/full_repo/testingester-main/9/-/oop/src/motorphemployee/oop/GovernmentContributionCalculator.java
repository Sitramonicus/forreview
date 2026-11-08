package motorphemployee.oop;

/**
 * GovernmentContributionCalculator - abstract base for every statutory deduction
 * (MS1 Week 3/4/5 CRC rows).
 *
 * PayrollCalculator works with THIS type only, never with SSSCalculator or any other
 * concrete subclass. Adding a new deduction later means adding a subclass and one line
 * in the calculator list - no change to the orchestrator.
 */
public abstract class GovernmentContributionCalculator {

    private final String contributionName;
    private final String contributionCode;
    private boolean isActive;

    protected GovernmentContributionCalculator(String contributionName, String contributionCode) {
        if (contributionName == null || contributionName.trim().isEmpty()) {
            throw new IllegalArgumentException("contributionName cannot be empty");
        }
        if (contributionCode == null || contributionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("contributionCode cannot be empty");
        }
        this.contributionName = contributionName.trim();
        this.contributionCode = contributionCode.trim();
        this.isActive = true;
    }

    /** The polymorphic entry point: every deduction type implements only this. */
    public abstract double calculate(double grossSalary);

    public String getContributionName() {
        return contributionName;
    }

    public String getContributionCode() {
        return contributionCode;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /** Shared guard: a contribution on an invalid salary is a bug, not a zero. */
    protected double requireValidSalary(double grossSalary) {
        if (Double.isNaN(grossSalary) || grossSalary < 0) {
            throw new IllegalArgumentException(contributionName + ": invalid salary " + grossSalary);
        }
        return grossSalary;
    }

    @Override
    public String toString() {
        return contributionName + " (" + contributionCode + ", " + (isActive ? "active" : "inactive") + ")";
    }
}
