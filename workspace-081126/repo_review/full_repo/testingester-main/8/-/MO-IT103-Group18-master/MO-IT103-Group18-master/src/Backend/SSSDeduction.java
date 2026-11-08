package Backend;

public class SSSDeduction extends DeductionCalculator {

    // SSS contribution bracket table: P135 up to P3,250, +P22.50 per P500 bracket,
    // capped at P4,500 (gives the same values as CP2's computeSSS() lookup table)
    @Override
    public double computeDeduction(double grossPay) {
        double amount;
        if (grossPay <= 3250) {
            amount = 135.0;
        } else if (grossPay > 99750) {
            amount = 4500.0;
        } else {
            int steps = (int) Math.ceil((grossPay - 3250) / 500.0);
            amount = 135.0 + (steps * 22.50);
        }
        return recordAmount(amount);
    }

    public static void main(String[] args) {
        SSSDeduction sss1 = new SSSDeduction();
        System.out.println("SSS Deduction (25,000): " + sss1.computeDeduction(25000.0));
        System.out.println("Stored amount: " + sss1.getDeductionAmount());
    }
}