package Backend;

public class WithholdingTaxDeduction extends DeductionCalculator {

    // BIR withholding tax bracket table. The argument is the tax basis chosen by the
    // caller (PayrollProcessor decides whether this is gross pay or taxable income).
    @Override
    public double computeDeduction(double grossPay) {
        double amount;
        if (grossPay <= 20833) {
            amount = 0;
        } else if (grossPay <= 33332) {
            amount = (grossPay - 20833) * 0.20;
        } else if (grossPay <= 66666) {
            amount = 2500 + (grossPay - 33332) * 0.25;
        } else if (grossPay <= 166666) {
            amount = 10833.33 + (grossPay - 66666) * 0.30;
        } else if (grossPay <= 666666) {
            amount = 40833.33 + (grossPay - 166666) * 0.32;
        } else {
            amount = 200833.33 + (grossPay - 666666) * 0.35;
        }
        return recordAmount(amount);
    }

    public static void main(String[] args) {
        WithholdingTaxDeduction tax = new WithholdingTaxDeduction();
        System.out.println("Withholding Tax (25,000): " + tax.computeDeduction(25000));
    }
}