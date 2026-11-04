package motorphpayrollsystem;

public class Payroll {

    // ── CSV column indices ────────────────────────────────────────────────────
    private static final int COL_BASIC = 13;
    private static final int COL_RICE  = 14;
    private static final int COL_PHONE = 15;
    private static final int COL_CLOTH = 16;
    private static final int COL_SEMI  = 17;
    private static final int COL_HOUR  = 18;

    // Standard working days and hours per month
    public static final double WORKING_DAYS_PER_MONTH  = 22.0;
    public static final double WORKING_HOURS_PER_DAY   = 8.0;
    public static final double WORKING_HOURS_PER_MONTH =
        WORKING_DAYS_PER_MONTH * WORKING_HOURS_PER_DAY; // 176

    // =========================================================================
    // ARITHMETIC RELATIONSHIPS
    // basicSalary = hourlyRate * 176
    // semiMonthlyRate = basicSalary / 2
    // =========================================================================
    public static double basicToSemiMonthly(double basicSalary) {
        return basicSalary / 2.0;
    }

    public static double basicToHourly(double basicSalary) {
        return basicSalary / WORKING_HOURS_PER_MONTH;
    }

    public static double hourlyToBasic(double hourlyRate) {
        return hourlyRate * WORKING_HOURS_PER_MONTH;
    }

    public static double semiMonthlyToBasic(double semiMonthly) {
        return semiMonthly * 2.0;
    }

    // =========================================================================
    // LEGACY METHODS — used by PayrollAdminUI (kept for compatibility)
    // =========================================================================

    /** Gross pay = semiMonthly + allowances */
    public static double grossPay(String[] emp) {
        return parseAmount(emp[COL_SEMI])
             + parseAmount(emp[COL_RICE])
             + parseAmount(emp[COL_PHONE])
             + parseAmount(emp[COL_CLOTH]);
    }

    public static double sssContribution(double grossPay) {
        return computeSSS(grossPay * 2) / 2;
    }

    public static double philHealthContribution(double grossPay) {
        return computePhilHealth(grossPay * 2) / 2;
    }

    public static double pagIbigContribution(double semiMonthlyRate) {
        return computePagIbig(semiMonthlyRate * 2) / 2;
    }

    public static double totalDeductions(double grossPay, double basicSalary) {
        return sssContribution(grossPay)
             + philHealthContribution(grossPay)
             + pagIbigContribution(basicSalary / 2);
    }

    public static double taxableIncome(double grossPay, double totalDeductions) {
        return Math.max(0, grossPay - totalDeductions);
    }

    public static double taxDeduction(double taxableIncome) {
        return computeWithholdingTax(taxableIncome * 2) / 2;
    }

    public static double NetPay(double grossPay, double totalDeductions,
                                double taxDeductions) {
        return Math.max(0, grossPay - totalDeductions - taxDeductions);
    }

    // =========================================================================
    // FULL COMPUTE — used by PayrollUI
    // Enforces the arithmetic relationship between basic, semi-monthly, hourly
    // =========================================================================
    public static PayrollResult compute(String[] emp, double hoursWorked) {
        PayrollResult r = new PayrollResult();

        double basic    = parseAmount(emp[COL_BASIC]);
        double rice     = parseAmount(emp[COL_RICE]);
        double phone    = parseAmount(emp[COL_PHONE]);
        double clothing = parseAmount(emp[COL_CLOTH]);

        // Enforce arithmetic: if hourly is missing/zero, compute from basic
        double hourly = parseAmount(emp[COL_HOUR]);
        if (hourly <= 0 && basic > 0) hourly = basicToHourly(basic);

        // Enforce: if semiMonthly is missing/zero, compute from basic
        double semi = parseAmount(emp[COL_SEMI]);
        if (semi <= 0 && basic > 0) semi = basicToSemiMonthly(basic);

        double allowances = rice + phone + clothing;
        double grossPay   = (hoursWorked * hourly) + allowances;

        // Deductions based on monthly basic (converted from semi)
        double monthly = semi * 2;
        double sss        = computeSSS(monthly)        / 2;
        double philHealth = computePhilHealth(monthly)  / 2;
        double pagIbig    = computePagIbig(monthly)     / 2;
        double taxable    = Math.max(0, grossPay - sss - philHealth - pagIbig);
        double tax        = computeWithholdingTax(taxable * 2) / 2;
        double totalDed   = sss + philHealth + pagIbig + tax;
        double netPay     = Math.max(0, grossPay - totalDed);

        r.empNo = emp[0]; r.lastName = emp[1]; r.firstName = emp[2];
        r.position = emp[11]; r.basicSalary = basic; r.hourlyRate = hourly;
        r.hoursWorked = hoursWorked; r.riceSubsidy = rice;
        r.phoneAllow = phone; r.clothingAllow = clothing;
        r.totalAllowances = allowances; r.grossPay = grossPay;
        r.sssDeduction = sss; r.philHealth = philHealth;
        r.pagIbig = pagIbig; r.withholdingTax = tax;
        r.totalDeductions = totalDed; r.netPay = netPay;
        return r;
    }

    // =========================================================================
    // STATUTORY DEDUCTION TABLES (Philippine 2024)
    // =========================================================================
    public static double computeSSS(double monthly) {
        if (monthly <  3250) return  135.00;
        if (monthly <  3750) return  157.50;
        if (monthly <  4250) return  180.00;
        if (monthly <  4750) return  202.50;
        if (monthly <  5250) return  225.00;
        if (monthly <  5750) return  247.50;
        if (monthly <  6250) return  270.00;
        if (monthly <  6750) return  292.50;
        if (monthly <  7250) return  315.00;
        if (monthly <  7750) return  337.50;
        if (monthly <  8250) return  360.00;
        if (monthly <  8750) return  382.50;
        if (monthly <  9250) return  405.00;
        if (monthly <  9750) return  427.50;
        if (monthly < 10250) return  450.00;
        if (monthly < 10750) return  472.50;
        if (monthly < 11250) return  495.00;
        if (monthly < 11750) return  517.50;
        if (monthly < 12250) return  540.00;
        if (monthly < 12750) return  562.50;
        if (monthly < 13250) return  585.00;
        if (monthly < 13750) return  607.50;
        if (monthly < 14250) return  630.00;
        if (monthly < 14750) return  652.50;
        if (monthly < 15250) return  675.00;
        if (monthly < 15750) return  697.50;
        if (monthly < 16250) return  720.00;
        if (monthly < 16750) return  742.50;
        if (monthly < 17250) return  765.00;
        if (monthly < 17750) return  787.50;
        if (monthly < 18250) return  810.00;
        if (monthly < 18750) return  832.50;
        if (monthly < 19250) return  855.00;
        if (monthly < 19750) return  877.50;
        if (monthly < 20250) return  900.00;
        if (monthly < 20750) return  922.50;
        if (monthly < 21250) return  945.00;
        if (monthly < 21750) return  967.50;
        if (monthly < 22250) return  990.00;
        if (monthly < 22750) return 1012.50;
        if (monthly < 23250) return 1035.00;
        if (monthly < 23750) return 1057.50;
        if (monthly < 24250) return 1080.00;
        if (monthly < 24750) return 1102.50;
        return 1125.00;
    }

    /** PhilHealth: 3% of monthly salary, employee pays half. Cap ₱1,800. */
    public static double computePhilHealth(double monthly) {
        return Math.min(monthly * 0.015, 900.00); // employee share = 1.5%
    }

    /** Pag-IBIG: 1% if ≤1500, else 2%, capped at ₱100 employee share. */
    public static double computePagIbig(double monthly) {
        if (monthly <= 1500) return monthly * 0.01;
        return Math.min(monthly * 0.02, 100.00);
    }

    /** BIR withholding tax (monthly taxable income). */
    public static double computeWithholdingTax(double taxable) {
        if (taxable <=  20833) return 0;
        if (taxable <=  33333) return (taxable - 20833)   * 0.20;
        if (taxable <=  66667) return 2500  + (taxable - 33333)  * 0.25;
        if (taxable <= 166667) return 10833 + (taxable - 66667)  * 0.30;
        if (taxable <= 666667) return 40833 + (taxable - 166667) * 0.32;
        return 200833 + (taxable - 666667) * 0.35;
    }

    // =========================================================================
    // PARSE — null-safe, comma-tolerant
    // =========================================================================
    public static double parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(raw.trim()
                .replace(",", "")
                .replace("₱", "")
                .replace("P", "")
                .trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // =========================================================================
    // RESULT CLASS
    // =========================================================================
    public static class PayrollResult {
        public String empNo, lastName, firstName, position;
        public double basicSalary, hourlyRate, hoursWorked;
        public double riceSubsidy, phoneAllow, clothingAllow, totalAllowances;
        public double grossPay, sssDeduction, philHealth, pagIbig, withholdingTax;
        public double totalDeductions, netPay;

        public String toPayslip() {
            String ls = System.lineSeparator(); // platform-safe line endings
            return String.format(
                "╔══════════════════════════════════════════╗" + ls +
                "║         MOTORPH PAYROLL SYSTEM           ║" + ls +
                "║              PAYSLIP                     ║" + ls +
                "╠══════════════════════════════════════════╣" + ls +
                "  Employee No : %s" + ls +
                "  Name        : %s, %s" + ls +
                "  Position    : %s" + ls +
                "──────────────────────────────────────────" + ls +
                "  EARNINGS" + ls +
                "  Basic Salary        : P%,.2f" + ls +
                "  Hourly Rate         : P%,.2f" + ls +
                "  Hours Worked        : %.2f hrs" + ls +
                "  Rice Subsidy        : P%,.2f" + ls +
                "  Phone Allowance     : P%,.2f" + ls +
                "  Clothing Allowance  : P%,.2f" + ls +
                "  GROSS PAY           : P%,.2f" + ls +
                "──────────────────────────────────────────" + ls +
                "  DEDUCTIONS" + ls +
                "  SSS                 : P%,.2f" + ls +
                "  PhilHealth          : P%,.2f" + ls +
                "  Pag-IBIG            : P%,.2f" + ls +
                "  Withholding Tax     : P%,.2f" + ls +
                "  TOTAL DEDUCTIONS    : P%,.2f" + ls +
                "══════════════════════════════════════════" + ls +
                "  NET PAY             : P%,.2f" + ls +
                "╚══════════════════════════════════════════╝",
                empNo, lastName, firstName, position,
                basicSalary, hourlyRate, hoursWorked,
                riceSubsidy, phoneAllow, clothingAllow, grossPay,
                sssDeduction, philHealth, pagIbig, withholdingTax,
                totalDeductions, netPay);
        }

        public Object[] toTableRow() {
            return new Object[]{
                empNo, lastName + ", " + firstName, position,
                String.format("%.2f", hoursWorked),
                String.format("P%,.2f", grossPay),
                String.format("P%,.2f", totalDeductions),
                String.format("P%,.2f", netPay)
            };
        }
    }
}
