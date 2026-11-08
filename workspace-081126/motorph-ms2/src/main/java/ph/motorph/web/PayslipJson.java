package ph.motorph.web;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

/** Single JSON mapping for a payslip, shared by storage and the HTTP layer. */
final class PayslipJson {
    private PayslipJson() { }

    static JsonObject toJson(Payslip slip) {
        JsonObject o = new JsonObject();
        o.addProperty("employeeId", slip.employeeId());
        o.addProperty("employeeName", slip.employeeName());
        o.addProperty("position", slip.position());
        o.addProperty("department", slip.department());
        o.addProperty("payPeriod", slip.payPeriod());
        o.addProperty("periodStart", slip.periodStart());
        o.addProperty("periodEnd", slip.periodEnd());
        o.addProperty("rulesetId", slip.rulesetId());
        money(o, "monthlyBasic", slip.monthlyBasic());
        money(o, "dailyRate", slip.dailyRate());
        number(o, "daysWorked", slip.daysWorked());
        money(o, "hourlyRate", slip.hourlyRate());
        number(o, "overtimeHours", slip.overtimeHours());
        money(o, "overtimePay", slip.overtimePay());
        number(o, "nightDifferentialHours", slip.nightDifferentialHours());
        money(o, "nightDifferentialPay", slip.nightDifferentialPay());
        money(o, "grossIncome", slip.grossIncome());
        money(o, "riceSubsidy", slip.riceSubsidy());
        money(o, "phoneAllowance", slip.phoneAllowance());
        money(o, "clothingAllowance", slip.clothingAllowance());
        money(o, "taxableAllowance", slip.taxableAllowance());
        money(o, "nonTaxableAllowance", slip.nonTaxableAllowance());
        money(o, "totalBenefits", slip.totalBenefits());
        money(o, "sssEmployee", slip.sssEmployee());
        money(o, "philHealthEmployee", slip.philHealthEmployee());
        money(o, "pagibigEmployee", slip.pagibigEmployee());
        money(o, "withholdingTax", slip.withholdingTax());
        money(o, "totalDeductions", slip.totalDeductions());
        money(o, "sssEmployer", slip.sssEmployer());
        money(o, "sssEc", slip.sssEc());
        money(o, "philHealthEmployer", slip.philHealthEmployer());
        money(o, "pagibigEmployer", slip.pagibigEmployer());
        money(o, "employerCost", slip.employerCost());
        money(o, "taxableIncome", slip.taxableIncome());
        money(o, "takeHomePay", slip.takeHomePay());
        return o;
    }

    static Payslip fromJson(JsonObject o) {
        return Payslip.builder()
            .employee(text(o, "employeeId"), text(o, "employeeName"), text(o, "position"), text(o, "department"))
            .period(text(o, "payPeriod"), text(o, "periodStart"), text(o, "periodEnd"))
            .ruleset(text(o, "rulesetId"))
            .salary(amount(o, "monthlyBasic"), amount(o, "dailyRate"), number(o, "daysWorked"), amount(o, "hourlyRate"))
            .premiums(number(o, "overtimeHours"), amount(o, "overtimePay"),
                number(o, "nightDifferentialHours"), amount(o, "nightDifferentialPay"), amount(o, "grossIncome"))
            .benefits(amount(o, "riceSubsidy"), amount(o, "phoneAllowance"), amount(o, "clothingAllowance"),
                amount(o, "taxableAllowance"), amount(o, "nonTaxableAllowance"), amount(o, "totalBenefits"))
            .deductions(amount(o, "sssEmployee"), amount(o, "philHealthEmployee"), amount(o, "pagibigEmployee"),
                amount(o, "withholdingTax"), amount(o, "totalDeductions"))
            .employer(amount(o, "sssEmployer"), amount(o, "sssEc"), amount(o, "philHealthEmployer"),
                amount(o, "pagibigEmployer"), amount(o, "employerCost"))
            .result(amount(o, "taxableIncome"), amount(o, "takeHomePay"))
            .build();
    }

    private static void money(JsonObject o, String key, BigDecimal value) {
        o.addProperty(key, Money.round(value).toPlainString());
    }

    private static void number(JsonObject o, String key, BigDecimal value) {
        o.addProperty(key, value.toPlainString());
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static BigDecimal amount(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? Money.parse(o.get(key).getAsString()) : Money.ZERO;
    }

    private static BigDecimal number(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull()
            ? new BigDecimal(o.get(key).getAsString()) : BigDecimal.ZERO;
    }
}
