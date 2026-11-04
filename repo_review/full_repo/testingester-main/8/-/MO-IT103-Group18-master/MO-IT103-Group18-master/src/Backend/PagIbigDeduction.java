package Backend;

public class PagIbigDeduction extends DeductionCalculator {

    // 2% of salary, capped at P100
    @Override
    public double computeDeduction(double grossPay) {
        return recordAmount(Math.min(grossPay * 0.02, 100));
    }

    public static void main(String[] args) {
        PagIbigDeduction pagIbig1 = new PagIbigDeduction();
        System.out.println("Pag-IBIG Deduction (25,000): " + pagIbig1.computeDeduction(25000.0));
    }
}