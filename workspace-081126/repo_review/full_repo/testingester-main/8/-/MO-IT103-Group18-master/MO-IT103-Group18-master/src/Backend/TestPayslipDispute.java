package Backend;

public class TestPayslipDispute {
    
    public static void main(String[] args) {

        PayslipDispute dispute = new PayslipDispute(
            "DSP-001",
            "10001",
            "October 2026",
            "Incorrect Deduction",
            "My payslip shows an incorrect deduction."
        );

        System.out.println(dispute);

        dispute.markUnderReview("PAYROLL-01");

        System.out.println("Status: " + dispute.getStatus());

        dispute.resolve(
            "PAYROLL-01",
            "Deduction verified and corrected."
        );

        System.out.println("Final Status: " + dispute.getStatus());
        System.out.println("Resolution: " + dispute.getResolutionNotes());
    }
}
