package Backend;

public abstract class PayrollProcessor {
    protected final Employee employee;

    private double netPay;

    public PayrollProcessor(Employee employee) {
        this.employee = employee;
    }

    protected abstract double computePay();

    // Subclasses override these to turn off deductions/allowances
    // when they don't apply (e.g. 13th month pay)
    protected boolean appliesStatutoryDeductions() {
        return true;
    }

    protected boolean appliesAllowances() {
        return true;
    }

    // Amount that SSS, PhilHealth and Pag-IBIG are computed on.
    // Default: the whole gross pay. FinalPayPayrollProcessor overrides it so
    // contributions apply only to the last salary, not to 13th month or leave conversion.
    protected double getContributionBase(double grossPay) {
        return grossPay;
    }

    // Income that withholding tax is computed on (taxable income).
    // Default: gross pay minus the three statutory contributions (group decision, Oct 27).
    protected double getTaxableIncome(double grossPay, double contributions) {
        return grossPay - contributions;
    }

    // Optional extra lines that explain how gross pay was built (shown on the payslip).
    // Default: none. FinalPayPayrollProcessor overrides it to list its components.
    public String getGrossPayBreakdown() {
        return "";
    }

    private double calculateNetPay(double grossPay, double deductions) {
        netPay = grossPay - deductions;
        return netPay;
    }

    public double getNetPay() { return netPay; }

    public Payslip processPayroll(String employeeId, String payPeriod) {
        double gross = computePay();

        double sss = 0;
        double philHealth = 0;
        double pagIbig = 0;
        double tax = 0;

        if (appliesStatutoryDeductions()) {
            double contributionBase = getContributionBase(gross);
            sss = new SSSDeduction().computeDeduction(contributionBase);
            philHealth = new PhilHealthDeduction().computeDeduction(contributionBase);
            pagIbig = new PagIbigDeduction().computeDeduction(contributionBase);
            double taxable = getTaxableIncome(gross, sss + philHealth + pagIbig);
            tax = new WithholdingTaxDeduction().computeDeduction(taxable);
        }

        double net = calculateNetPay(gross, sss + philHealth + pagIbig + tax);

        double riceSubsidy = appliesAllowances() ? employee.getRiceSubsidy() : 0;
        double phoneAllowance = appliesAllowances() ? employee.getPhoneAllowance() : 0;
        double clothingAllowance = appliesAllowances() ? employee.getClothingAllowance() : 0;

        return new Payslip(employeeId, payPeriod, gross, sss, philHealth, pagIbig, tax, net,
                riceSubsidy, phoneAllowance, clothingAllowance);
    }
}