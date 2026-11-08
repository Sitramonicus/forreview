package Backend;

public class PhilHealthDeduction extends DeductionCalculator {

    // Employee share: 3% of salary split 50/50, capped at P900
    @Override
    public double computeDeduction(double grossPay) {
        double amount = (grossPay >= 60000) ? 900.0 : grossPay * 0.03 / 2;
        return recordAmount(amount);
    }

    public static void main(String[] args) {
        PhilHealthDeduction philHealth1 = new PhilHealthDeduction();
        System.out.println("PhilHealth Deduction (25,000): " + philHealth1.computeDeduction(25000.0));
    }
}