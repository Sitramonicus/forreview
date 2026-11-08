package ph.motorph.web;

import java.math.BigDecimal;

/**
 * One computed payslip. Immutable: it records the inputs, the ruleset version and every
 * derived amount so a stored run can be re-explained later without re-running arithmetic.
 */
public final class Payslip {
    private final String employeeId;
    private final String employeeName;
    private final String position;
    private final String department;
    private final String payPeriod;
    private final String periodStart;
    private final String periodEnd;
    private final String rulesetId;

    private final BigDecimal monthlyBasic;
    private final BigDecimal dailyRate;
    private final BigDecimal daysWorked;
    private final BigDecimal hourlyRate;
    private final BigDecimal overtimeHours;
    private final BigDecimal overtimePay;
    private final BigDecimal nightDifferentialHours;
    private final BigDecimal nightDifferentialPay;
    private final BigDecimal grossIncome;

    private final BigDecimal riceSubsidy;
    private final BigDecimal phoneAllowance;
    private final BigDecimal clothingAllowance;
    private final BigDecimal taxableAllowance;
    private final BigDecimal nonTaxableAllowance;
    private final BigDecimal totalBenefits;

    private final BigDecimal sssEmployee;
    private final BigDecimal philHealthEmployee;
    private final BigDecimal pagibigEmployee;
    private final BigDecimal withholdingTax;
    private final BigDecimal totalDeductions;

    private final BigDecimal sssEmployer;
    private final BigDecimal sssEc;
    private final BigDecimal philHealthEmployer;
    private final BigDecimal pagibigEmployer;
    private final BigDecimal employerCost;

    private final BigDecimal taxableIncome;
    private final BigDecimal takeHomePay;

    private Payslip(Builder b) {
        this.employeeId = b.employeeId;
        this.employeeName = b.employeeName;
        this.position = b.position;
        this.department = b.department;
        this.payPeriod = b.payPeriod;
        this.periodStart = b.periodStart;
        this.periodEnd = b.periodEnd;
        this.rulesetId = b.rulesetId;
        this.monthlyBasic = b.monthlyBasic;
        this.dailyRate = b.dailyRate;
        this.daysWorked = b.daysWorked;
        this.hourlyRate = b.hourlyRate;
        this.overtimeHours = b.overtimeHours;
        this.overtimePay = b.overtimePay;
        this.nightDifferentialHours = b.nightDifferentialHours;
        this.nightDifferentialPay = b.nightDifferentialPay;
        this.grossIncome = b.grossIncome;
        this.riceSubsidy = b.riceSubsidy;
        this.phoneAllowance = b.phoneAllowance;
        this.clothingAllowance = b.clothingAllowance;
        this.taxableAllowance = b.taxableAllowance;
        this.nonTaxableAllowance = b.nonTaxableAllowance;
        this.totalBenefits = b.totalBenefits;
        this.sssEmployee = b.sssEmployee;
        this.philHealthEmployee = b.philHealthEmployee;
        this.pagibigEmployee = b.pagibigEmployee;
        this.withholdingTax = b.withholdingTax;
        this.totalDeductions = b.totalDeductions;
        this.sssEmployer = b.sssEmployer;
        this.sssEc = b.sssEc;
        this.philHealthEmployer = b.philHealthEmployer;
        this.pagibigEmployer = b.pagibigEmployer;
        this.employerCost = b.employerCost;
        this.taxableIncome = b.taxableIncome;
        this.takeHomePay = b.takeHomePay;
    }

    public String employeeId() { return employeeId; }
    public String employeeName() { return employeeName; }
    public String position() { return position; }
    public String department() { return department; }
    public String payPeriod() { return payPeriod; }
    public String periodStart() { return periodStart; }
    public String periodEnd() { return periodEnd; }
    public String rulesetId() { return rulesetId; }
    public BigDecimal monthlyBasic() { return monthlyBasic; }
    public BigDecimal dailyRate() { return dailyRate; }
    public BigDecimal daysWorked() { return daysWorked; }
    public BigDecimal hourlyRate() { return hourlyRate; }
    public BigDecimal overtimeHours() { return overtimeHours; }
    public BigDecimal overtimePay() { return overtimePay; }
    public BigDecimal nightDifferentialHours() { return nightDifferentialHours; }
    public BigDecimal nightDifferentialPay() { return nightDifferentialPay; }
    public BigDecimal grossIncome() { return grossIncome; }
    public BigDecimal riceSubsidy() { return riceSubsidy; }
    public BigDecimal phoneAllowance() { return phoneAllowance; }
    public BigDecimal clothingAllowance() { return clothingAllowance; }
    public BigDecimal taxableAllowance() { return taxableAllowance; }
    public BigDecimal nonTaxableAllowance() { return nonTaxableAllowance; }
    public BigDecimal totalBenefits() { return totalBenefits; }
    public BigDecimal sssEmployee() { return sssEmployee; }
    public BigDecimal philHealthEmployee() { return philHealthEmployee; }
    public BigDecimal pagibigEmployee() { return pagibigEmployee; }
    public BigDecimal withholdingTax() { return withholdingTax; }
    public BigDecimal totalDeductions() { return totalDeductions; }
    public BigDecimal sssEmployer() { return sssEmployer; }
    public BigDecimal sssEc() { return sssEc; }
    public BigDecimal philHealthEmployer() { return philHealthEmployer; }
    public BigDecimal pagibigEmployer() { return pagibigEmployer; }
    public BigDecimal employerCost() { return employerCost; }
    public BigDecimal taxableIncome() { return taxableIncome; }
    public BigDecimal takeHomePay() { return takeHomePay; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String employeeId;
        private String employeeName = "";
        private String position = "";
        private String department = "";
        private String payPeriod = "MONTHLY";
        private String periodStart = "";
        private String periodEnd = "";
        private String rulesetId = "";
        private BigDecimal monthlyBasic = Money.ZERO;
        private BigDecimal dailyRate = Money.ZERO;
        private BigDecimal daysWorked = BigDecimal.ZERO;
        private BigDecimal hourlyRate = Money.ZERO;
        private BigDecimal overtimeHours = BigDecimal.ZERO;
        private BigDecimal overtimePay = Money.ZERO;
        private BigDecimal nightDifferentialHours = BigDecimal.ZERO;
        private BigDecimal nightDifferentialPay = Money.ZERO;
        private BigDecimal grossIncome = Money.ZERO;
        private BigDecimal riceSubsidy = Money.ZERO;
        private BigDecimal phoneAllowance = Money.ZERO;
        private BigDecimal clothingAllowance = Money.ZERO;
        private BigDecimal taxableAllowance = Money.ZERO;
        private BigDecimal nonTaxableAllowance = Money.ZERO;
        private BigDecimal totalBenefits = Money.ZERO;
        private BigDecimal sssEmployee = Money.ZERO;
        private BigDecimal philHealthEmployee = Money.ZERO;
        private BigDecimal pagibigEmployee = Money.ZERO;
        private BigDecimal withholdingTax = Money.ZERO;
        private BigDecimal totalDeductions = Money.ZERO;
        private BigDecimal sssEmployer = Money.ZERO;
        private BigDecimal sssEc = Money.ZERO;
        private BigDecimal philHealthEmployer = Money.ZERO;
        private BigDecimal pagibigEmployer = Money.ZERO;
        private BigDecimal employerCost = Money.ZERO;
        private BigDecimal taxableIncome = Money.ZERO;
        private BigDecimal takeHomePay = Money.ZERO;

        public Builder employee(String id, String name, String position, String department) {
            this.employeeId = id; this.employeeName = name;
            this.position = position; this.department = department;
            return this;
        }
        public Builder period(String payPeriod, String start, String end) {
            this.payPeriod = payPeriod; this.periodStart = start; this.periodEnd = end; return this;
        }
        public Builder ruleset(String id) { this.rulesetId = id; return this; }
        public Builder salary(BigDecimal monthlyBasic, BigDecimal dailyRate, BigDecimal daysWorked, BigDecimal hourlyRate) {
            this.monthlyBasic = monthlyBasic; this.dailyRate = dailyRate;
            this.daysWorked = daysWorked; this.hourlyRate = hourlyRate; return this;
        }
        public Builder premiums(BigDecimal overtimeHours, BigDecimal overtimePay,
                                BigDecimal ndHours, BigDecimal ndPay, BigDecimal gross) {
            this.overtimeHours = overtimeHours; this.overtimePay = overtimePay;
            this.nightDifferentialHours = ndHours; this.nightDifferentialPay = ndPay;
            this.grossIncome = gross; return this;
        }
        public Builder benefits(BigDecimal rice, BigDecimal phone, BigDecimal clothing,
                                BigDecimal taxable, BigDecimal nonTaxable, BigDecimal total) {
            this.riceSubsidy = rice; this.phoneAllowance = phone; this.clothingAllowance = clothing;
            this.taxableAllowance = taxable; this.nonTaxableAllowance = nonTaxable;
            this.totalBenefits = total; return this;
        }
        public Builder deductions(BigDecimal sss, BigDecimal philHealth, BigDecimal pagibig,
                                  BigDecimal tax, BigDecimal total) {
            this.sssEmployee = sss; this.philHealthEmployee = philHealth;
            this.pagibigEmployee = pagibig; this.withholdingTax = tax;
            this.totalDeductions = total; return this;
        }
        public Builder employer(BigDecimal sss, BigDecimal ec, BigDecimal philHealth,
                                BigDecimal pagibig, BigDecimal totalCost) {
            this.sssEmployer = sss; this.sssEc = ec; this.philHealthEmployer = philHealth;
            this.pagibigEmployer = pagibig; this.employerCost = totalCost; return this;
        }
        public Builder result(BigDecimal taxableIncome, BigDecimal takeHomePay) {
            this.taxableIncome = taxableIncome; this.takeHomePay = takeHomePay; return this;
        }
        public Payslip build() {
            if (employeeId == null || employeeId.trim().isEmpty()) throw new IllegalStateException("employee id is required");
            return new Payslip(this);
        }
    }
}
