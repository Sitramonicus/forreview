package Backend;

// Abstract contract for statutory deductions. grossPay is passed into computeDeduction()
// rather than fixed in the constructor, so the caller (PayrollProcessor) controls the
// basis for each deduction. For example, withholding tax can be given taxable income
// (gross pay minus SSS, PhilHealth, and Pag-IBIG) instead of the unadjusted gross pay.
public abstract class DeductionCalculator {
    private double deductionAmount;

    // Read-only; the amount is derived internally by computeDeduction()
    public double getDeductionAmount() {
        return deductionAmount;
    }

    // Lets subclasses store the result they computed; outside classes cannot set it
    protected double recordAmount(double amount) {
        this.deductionAmount = amount;
        return amount;
    }

    public abstract double computeDeduction(double grossPay);
}