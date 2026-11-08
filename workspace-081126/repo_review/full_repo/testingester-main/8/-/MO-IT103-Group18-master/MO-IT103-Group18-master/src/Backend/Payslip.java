package Backend;

public class Payslip {
    private final String employeeId;
    private final String payPeriod;
    private final double grossPay;
    private final double sssDeduction;
    private final double philHealthDeduction;
    private final double pagIbigDeduction;
    private final double withholdingTax;
    private final double netPay;
    private final double riceSubsidy;
    private final double phoneAllowance;
    private final double clothingAllowance;
    private final double takeHomePay;

    public Payslip(String employeeId, String payPeriod, double grossPay,
                    double sssDeduction, double philHealthDeduction, double pagIbigDeduction,
                    double withholdingTax, double netPay,
                    double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        this.employeeId = employeeId;
        this.payPeriod = payPeriod;
        this.grossPay = grossPay;
        this.sssDeduction = sssDeduction;
        this.philHealthDeduction = philHealthDeduction;
        this.pagIbigDeduction = pagIbigDeduction;
        this.withholdingTax = withholdingTax;
        this.netPay = netPay;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
        // Allowances are tax-exempt, so they're added AFTER net pay,
        // not part of the taxable gross pay used earlier
        this.takeHomePay = netPay + riceSubsidy + phoneAllowance + clothingAllowance;
    }

    public String getEmployeeId() { return employeeId; }
    public String getPayPeriod() { return payPeriod; }
    public double getGrossPay() { return grossPay; }
    public double getSssDeduction() { return sssDeduction; }
    public double getPhilHealthDeduction() { return philHealthDeduction; }
    public double getPagIbigDeduction() { return pagIbigDeduction; }
    public double getWithholdingTax() { return withholdingTax; }
    public double getNetPay() { return netPay; }
    public double getRiceSubsidy() { return riceSubsidy; }
    public double getPhoneAllowance() { return phoneAllowance; }
    public double getClothingAllowance() { return clothingAllowance; }
    public double getTakeHomePay() { return takeHomePay; }

    @Override
    public String toString() {
        return "Payslip for " + employeeId + " (" + payPeriod + ")\n"
                + "Gross Pay: " + grossPay + "\n"
                + "SSS: " + sssDeduction + "\n"
                + "PhilHealth: " + philHealthDeduction + "\n"
                + "Pag-IBIG: " + pagIbigDeduction + "\n"
                + "Withholding Tax: " + withholdingTax + "\n"
                + "Net Pay: " + netPay + "\n"
                + "Rice Subsidy: " + riceSubsidy + "\n"
                + "Phone Allowance: " + phoneAllowance + "\n"
                + "Clothing Allowance: " + clothingAllowance + "\n"
                + "Take-Home Pay: " + takeHomePay;
    }
}