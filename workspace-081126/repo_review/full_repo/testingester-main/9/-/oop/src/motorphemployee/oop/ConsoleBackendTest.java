package motorphemployee.oop;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ConsoleBackendTest - the mandatory console test for Week 7-8 backend work.
 *
 * NOT a production class: this is the harness that proves the backend behaves before any
 * GUI is touched (MS2 instructions: "Console Testing ... Test each method individually in
 * the console before touching the GUI"). It prints PASS/FAIL lines and exits with code 1
 * when anything fails, so a broken build cannot be mistaken for a working one.
 *
 * The PARITY lines are machine-comparable with the CP2 baseline: the same input salary is
 * pushed through the CP2 static formulas (Cp2Parity) and through the OOP calculators. Since 2026-10-27 the OOP values follow the OFFICIAL government matrices, so a CP2
 * mismatch is EXPECTED and documented (oop/matrix-verification.md) - section H proves the
 * OOP numbers against the official values instead.
 */
public final class ConsoleBackendTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== MotorPH OOP backend console test (Waves 1-3) ===");
        System.out.println("java.version = " + System.getProperty("java.version"));
        System.out.println();

        section("A. Inheritance + polymorphism (Employee reference, two real types)");
        testPolymorphicBenefits();

        section("B. Encapsulation / validation (the CP2 holes)");
        testValidation();

        section("C. Government contribution calculators through the abstract type");
        testCalculatorParityInputs();

        section("D. AttendanceLog integrity");
        testAttendance();

        section("E. PayrollRecord immutability + internal consistency");
        testPayrollRecord();

        section("F. Attendance -> gross -> deductions -> net (end-to-end backend run)");
        testEndToEndNetPay();

        section("G. Known issue surfaced by this run: negative net pay");
        System.out.println("  At gross pay 0.00 the statutory minimums are still charged:");
        System.out.println("    SSS = 135.00 + PhilHealth = 150.00 + Pag-IBIG = 0.00  ->  285.00 of deductions");
        System.out.println("  CP2 behaviour : tax on -285.00 = 0.00, so net pay = -285.00 (a negative payslip)");
        System.out.println("  OOP behaviour : the calculators refuse a negative taxable income instead");
        System.out.println("  -> logged as an open issue; the fix is a non-positive gross rejected by");
        System.out.println("     PayrollCalculator.calculatePayroll before any deduction is applied.");

        section("H. Official matrix conformance (2026-10-27)");
        testOfficialMatrixConformance();

        section("I. PayrollCalculator orchestrator - attendance in, immutable PayrollRecord out");
        testPayrollCalculatorOrchestrator();

        System.out.println();
        System.out.println("=== SUMMARY: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.out.println("RESULT: FAIL");
            System.exit(1);
        }
        System.out.println("RESULT: PASS");
    }

    // ------------------------------------------------------------------ A

    private static void testPolymorphicBenefits() {
        List<Employee> employees = new ArrayList<>();
        employees.add(new RegularEmployee(10001, "Pore", "Paul", "1990-02-14", "Manila", "09171234567",
                "34-1234567-8", "12-345678901-2", "123-456-789-000", "1234-5678-9012",
                "Regular", "Software Engineer", "Valencia", 35000.00, 1500, 2000, 1000, 17500, 205.36, 3));
        employees.add(new ProbationaryEmployee(10034, "Racimo", "Ana", "1998-07-01", "Quezon City", "09181234567",
                "34-9876543-2", "12-987654321-9", "987-654-321-000", "9876-5432-1098",
                "Probationary", "Junior Developer", "Pore", 22000.00, 1000, 500, 800, 11000, 129.08,
                5, "2026-06-01", 88.5));

        for (Employee employee : employees) {
            System.out.println("  " + employee);
            System.out.println("    getRiceSubsidy()          = " + money(employee.getRiceSubsidy()));
            System.out.println("    getPhoneAllowance()       = " + money(employee.getPhoneAllowance()));
            System.out.println("    getAnnualLeaveCredits()   = " + employee.getAnnualLeaveCredits());
            System.out.println("    isEligibleForPromotion()  = " + employee.isEligibleForPromotion());
            System.out.println("    getFullName()             = " + employee.getFullName() + "  (inherited from Person)");
            System.out.println("    getAge()                  = " + employee.getAge() + "  (inherited from Person)");
        }

        Employee regular = employees.get(0);
        Employee probationary = employees.get(1);

        check("RegularEmployee.getRiceSubsidy() == 1500 via Employee reference", regular.getRiceSubsidy() == 1500.0);
        check("RegularEmployee.getPhoneAllowance() == 2000", regular.getPhoneAllowance() == 2000.0);
        check("RegularEmployee.getAnnualLeaveCredits() == 12", regular.getAnnualLeaveCredits() == 12);
        check("RegularEmployee(3 yrs).isEligibleForPromotion() == true", regular.isEligibleForPromotion());
        check("ProbationaryEmployee.getRiceSubsidy() == 1000", probationary.getRiceSubsidy() == 1000.0);
        check("ProbationaryEmployee.getPhoneAllowance() == 500", probationary.getPhoneAllowance() == 500.0);
        check("ProbationaryEmployee.getAnnualLeaveCredits() == 6", probationary.getAnnualLeaveCredits() == 6);
        check("ProbationaryEmployee.isEligibleForPromotion() == false", !probationary.isEligibleForPromotion());
        check("Same call, different result - no if/else on employee type", regular.getRiceSubsidy() != probationary.getRiceSubsidy());
    }

    // ------------------------------------------------------------------ B

    private static void testValidation() {
        Employee employee = sampleRegular();

        expectThrows("setBasicSalary(-5000) rejected (CP2 accepted it - Employee.java:186)",
                () -> employee.setBasicSalary(-5000));
        expectThrows("setBasicSalary(0) rejected", () -> employee.setBasicSalary(0));
        expectThrows("setHourlyRate(0) rejected", () -> employee.setHourlyRate(0));
        expectThrows("setHourlyRate(-120) rejected", () -> employee.setHourlyRate(-120));
        expectThrows("setFirstName(\"  \") rejected", () -> employee.setFirstName("  "));
        expectThrows("setLastName(null) rejected", () -> employee.setLastName(null));
        expectThrows("setPhoneNumber(\"abc\") rejected (format check)", () -> employee.setPhoneNumber("abc"));
        expectThrows("setId(0) rejected", () -> employee.setId(0));
        expectThrows("setBirthday(future date) rejected", () -> employee.setBirthday("2099-01-01"));
        expectThrows("setGrossSemiMonthlyRate(-1) rejected", () -> employee.setGrossSemiMonthlyRate(-1));
        expectThrows("setClothingAllowance(-1) rejected", () -> employee.setClothingAllowance(-1));

        // Government IDs are read-only after creation: prove there is no setter at all.
        String[] forbiddenSetters = {"setSssNumber", "setPhilhealthNumber", "setTinNumber", "setPagIbigNumber"};
        boolean govIdSetterExists = false;
        for (Method method : Employee.class.getMethods()) {
            for (String forbidden : forbiddenSetters) {
                if (method.getName().equals(forbidden)) {
                    govIdSetterExists = true;
                }
            }
        }
        check("no setter exists for SSS/PhilHealth/TIN/Pag-IBIG numbers", !govIdSetterExists);

        // Valid values still work after the rejected ones (state was not corrupted).
        employee.setBasicSalary(36000);
        check("state intact, valid setBasicSalary(36000) accepted", employee.getBasicSalary() == 36000.0);
    }

    // ------------------------------------------------------------------ C

    private static void testCalculatorParityInputs() {
        List<GovernmentContributionCalculator> calculators = new ArrayList<>();
        calculators.add(new SSSCalculator());
        calculators.add(new PhilHealthCalculator());
        calculators.add(new PagIbigCalculator());

        System.out.println("  Calculators registered through the abstract type:");
        for (GovernmentContributionCalculator calculator : calculators) {
            System.out.println("    - " + calculator);
        }
        WithholdingTaxCalculator tax = new WithholdingTaxCalculator();
        System.out.println("    - " + tax + "   (applied after the three above)");
        System.out.println();
        System.out.println("  PARITY|<gross>|<sss>|<philhealth>|<pagibig>|<tax on (gross - sss-ph-ibig)>");

        for (double gross : parityInputs()) {
            double sss = 0;
            double philhealth = 0;
            double pagibig = 0;
            for (GovernmentContributionCalculator calculator : calculators) {
                double amount = calculator.calculate(gross);
                if (calculator instanceof SSSCalculator) {
                    sss = amount;
                } else if (calculator instanceof PhilHealthCalculator) {
                    philhealth = amount;
                } else {
                    pagibig = amount;
                }
            }
            double taxableIncome = gross - sss - philhealth - pagibig;
            if (taxableIncome < 0) {
                System.out.printf(Locale.ROOT,
                        "  PARITY_SKIPPED|%.2f|%.6f|%.6f|%.6f|reason=negative taxable income %.2f%n",
                        gross, sss, philhealth, pagibig, taxableIncome);
                continue;
            }
            double withholding = tax.calculate(taxableIncome);
            System.out.printf(Locale.ROOT, "  PARITY|%.2f|%.6f|%.6f|%.6f|%.6f%n",
                    gross, sss, philhealth, pagibig, withholding);
        }

        check("SSSCalculator caps at 1125.00", new SSSCalculator().calculate(999999) == 1125.0);
        check("SSSCalculator bracket count == 45", new SSSCalculator().getBracketCount() == 45);
        check("PhilHealthCalculator employee-share floor 150.00", new PhilHealthCalculator().calculate(9000) == 150.0);
        check("PhilHealthCalculator employee-share cap 900.00", new PhilHealthCalculator().calculate(999999) == 900.0);
        check("PagIbigCalculator cap 100.00", new PagIbigCalculator().calculate(999999) == 100.0);
        check("PagIbigCalculator 0 below 1000 salary (matrix silent -> UNVERIFIED)",
                new PagIbigCalculator().calculate(900) == 0.0);
        check("PagIbigCalculator charges the employee share only (2,500 -> 50.00)",
                new PagIbigCalculator().calculate(2500) == 50.0);
        check("WithholdingTaxCalculator 0 at/below 20832", new WithholdingTaxCalculator().calculate(20832) == 0.0);
        check("WithholdingTaxCalculator never negative in the 20,832-20,833 gap",
                new WithholdingTaxCalculator().calculate(20832.50) >= 0.0);
        expectThrows("calculate(-1) rejected by every calculator", () -> new SSSCalculator().calculate(-1));
        expectThrows("negative taxable income refused instead of paying a negative net",
                () -> new WithholdingTaxCalculator().calculate(-435.0));
    }

    private static List<Double> parityInputs() {
        List<Double> inputs = new ArrayList<>();
        double[] values = {
                0.00, 900.00, 1000.00, 1500.00, 1500.01, 2400.00,
                3250.00, 3250.01, 4000.00, 8000.00, 10000.00, 10000.01,
                12345.67, 15000.00, 20832.00, 20832.01, 25000.00, 33333.00,
                45000.00, 59999.99, 60000.00, 66667.00, 100000.00,
                166667.00, 666667.67, 1000000.00
        };
        for (double value : values) {
            inputs.add(value);
        }
        return inputs;
    }

    // ------------------------------------------------------------------ D

    private static void testAttendance() {
        AttendanceLog log = new AttendanceLog();

        log.recordClockIn(10001, "2024-06-03", "08:00:00");
        log.recordClockOut(10001, "2024-06-03", "17:00:00");
        log.recordClockIn(10001, "2024-06-04", "08:15:00");
        log.recordClockOut(10001, "2024-06-04", "16:45:00");
        log.recordClockIn(10001, "2024-06-05", "09:00:00");

        System.out.println("  daily hours 2024-06-03 = " + log.getDailyHours(10001, "2024-06-03"));
        System.out.println("  daily hours 2024-06-04 = " + log.getDailyHours(10001, "2024-06-04"));
        System.out.println("  daily hours 2024-06-05 = " + log.getDailyHours(10001, "2024-06-05") + " (no clock-out yet)");
        System.out.println("  worked days 2024-06-01..2024-06-30 = " + log.getWorkedDays(10001, "2024-06-01", "2024-06-30"));

        check("getDailyHours(2024-06-03) == 9.0", log.getDailyHours(10001, "2024-06-03") == 9.0);
        check("getDailyHours(2024-06-04) == 8.5", log.getDailyHours(10001, "2024-06-04") == 8.5);
        check("incomplete day counts as 0 hours", log.getDailyHours(10001, "2024-06-05") == 0.0);
        check("getWorkedDays(June) == 2", log.getWorkedDays(10001, "2024-06-01", "2024-06-30") == 2);
        expectThrows("duplicate clock-in rejected",
                () -> log.recordClockIn(10001, "2024-06-03", "08:05:00"));
        expectThrows("clock-out before clock-in rejected",
                () -> log.recordClockOut(10001, "2024-06-05", "08:00:00"));
        expectThrows("clock-out without clock-in rejected",
                () -> log.recordClockOut(10001, "2024-06-06", "17:00:00"));
        expectThrows("future-dated clock-in rejected",
                () -> log.recordClockIn(10001, "2099-01-01", "08:00:00"));
        expectThrows("unknown employee rejected", () -> log.getWorkedDays(0, "2024-06-01", "2024-06-30"));
    }

    // ------------------------------------------------------------------ E

    private static void testPayrollRecord() {
        ZonedDateTime now = ZonedDateTime.now();
        PayrollRecord record = new PayrollRecord(10001, now, now.minusDays(15), now,
                35000, 2000, 1500, 1000, 35000, 17500, 31000, 1125, 750, 100, 2025, 5, 160, 205.36, 0);

        System.out.println("  " + record);
        check("PayrollRecord keeps netIncome", record.getNetIncome() == 31000);
        check("getTotalDeductions() == 4000", record.getTotalDeductions() == 4000.0);

        boolean setterFound = false;
        for (Method method : PayrollRecord.class.getDeclaredMethods()) {
            if (method.getName().startsWith("set")) {
                setterFound = true;
            }
        }
        check("PayrollRecord has no setters (immutable)", !setterFound);

        expectThrows("inconsistent netIncome rejected at construction", () -> new PayrollRecord(
                10001, now, now.minusDays(15), now, 35000, 2000, 1500, 1000,
                35000, 17500, 999999, 1125, 750, 100, 2025, 5, 160, 205.36, 0));
        expectThrows("negative deduction rejected at construction", () -> new PayrollRecord(
                10001, now, now.minusDays(15), now, 35000, 2000, 1500, 1000,
                35000, 17500, 35000, -1125, 750, 100, 2025, 5, 160, 205.36, 0));
        expectThrows("periodEnd before periodStart rejected", () -> new PayrollRecord(
                10001, now, now, now.minusDays(15), 35000, 2000, 1500, 1000,
                35000, 17500, 35000, 1125, 750, 100, 2025, 5, 160, 205.36, 0));
    }

    // ------------------------------------------------------------------ F

    private static void testEndToEndNetPay() {
        AttendanceLog log = new AttendanceLog();
        int employeeId = 10001;
        // 20 complete working days (day 3 to day 22 of June 2024) - a realistic semi-monthly input.
        for (int day = 3; day <= 22; day++) {
            String date = String.format(Locale.ROOT, "2024-06-%02d", day);
            log.recordClockIn(employeeId, date, "08:00:00");
            log.recordClockOut(employeeId, date, "17:00:00");
        }

        Employee employee = sampleRegular();
        double hourlyRate = employee.getHourlyRate();
        int workedDays = log.getWorkedDays(employeeId, "2024-06-01", "2024-06-30");
        double gross = workedDays * hourlyRate * 8;

        List<GovernmentContributionCalculator> calculators = new ArrayList<>();
        calculators.add(new SSSCalculator());
        calculators.add(new PhilHealthCalculator());
        calculators.add(new PagIbigCalculator());

        double deductions = 0;
        for (GovernmentContributionCalculator calculator : calculators) {
            double amount = calculator.calculate(gross);
            System.out.println("  " + calculator.getContributionName() + " = " + money(amount));
            deductions += amount;
        }
        double taxableIncome = gross - deductions;
        double tax = new WithholdingTaxCalculator().calculate(taxableIncome);
        double net = taxableIncome - tax;

        System.out.println("  worked days      = " + workedDays);
        System.out.println("  hourly rate      = " + money(hourlyRate));
        System.out.println("  gross pay        = " + money(gross));
        System.out.println("  total deductions = " + money(deductions + tax));
        System.out.println("  withholding tax  = " + money(tax));
        System.out.println("  net pay          = " + money(net));

        check("worked days == 20", workedDays == 20);
        check("gross == 20 * 12 * 8", Math.abs(gross - 1920.0) < 0.000001);
        check("net pay == gross - all deductions",
                Math.abs(net - (gross - deductions - tax)) < 0.000001);
        check("net pay is positive and lower than gross", net > 0 && net < gross);
    }

    // ------------------------------------------------------------------ H

    /**
     * Since 2026-10-27 every expected value below is copied from the official matrix
     * (or from its SAMPLE block), not from CP2. Sources: Home_SSS_Matrix.xlsx, sheet
     * "SSS Contribution Schedule"; Home_Philhealth_Matrix.xlsx (note A6, SAMPLE B10);
     * Home_PagIBIG_Matrix.xlsx (B2/B3 employee rates, cap note A5);
     * Home_WithholdingTax_Matrix.xlsx (bracket text B3, SAMPLE B20).
     */
    private static void testOfficialMatrixConformance() {
        SSSCalculator sss = new SSSCalculator();
        checkCloseTo("SSS below the table (3,249.99)", sss.calculate(3249.99), 135.00);
        checkCloseTo("SSS first bracket is LOWER-bound inclusive (3,250)", sss.calculate(3250.00), 157.50);
        checkCloseTo("SSS 5,000", sss.calculate(5000.00), 225.00);
        checkCloseTo("SSS 10,000", sss.calculate(10000.00), 450.00);
        checkCloseTo("SSS 20,000", sss.calculate(20000.00), 900.00);
        checkCloseTo("SSS 24,750 (over the table)", sss.calculate(24750.00), 1125.00);

        PhilHealthCalculator philhealth = new PhilHealthCalculator();
        checkCloseTo("PhilHealth 9,000 (premium floor 300 -> employee 150)", philhealth.calculate(9000.00), 150.00);
        checkCloseTo("PhilHealth 15,000", philhealth.calculate(15000.00), 225.00);
        checkCloseTo("PhilHealth 25,000 matches SAMPLE B10 = 375", philhealth.calculate(25000.00), 375.00);
        checkCloseTo("PhilHealth 60,000 (ceiling 1,800 -> employee 900)", philhealth.calculate(60000.00), 900.00);

        PagIbigCalculator pagibig = new PagIbigCalculator();
        checkCloseTo("Pag-IBIG 1,000 (1% employee)", pagibig.calculate(1000.00), 10.00);
        checkCloseTo("Pag-IBIG 1,500 (1% employee)", pagibig.calculate(1500.00), 15.00);
        checkCloseTo("Pag-IBIG 2,500 (2% employee, no employer share)", pagibig.calculate(2500.00), 50.00);
        checkCloseTo("Pag-IBIG 5,000 (cap 100 reached by the employee share)", pagibig.calculate(5000.00), 100.00);

        WithholdingTaxCalculator tax = new WithholdingTaxCalculator();
        checkCloseTo("WHT 20,832 (exempt ceiling)", tax.calculate(20832.00), 0.00);
        checkCloseTo("WHT 23,400 matches SAMPLE B20 = 513.40", tax.calculate(23400.00), 513.40);
        checkCloseTo("WHT 25,000 (20,833 excess base)", tax.calculate(25000.00), 833.40);
        checkCloseTo("WHT 33,333 (bracket 2 floor)", tax.calculate(33333.00), 2500.00);
        checkCloseTo("WHT 40,000", tax.calculate(40000.00), 4166.75);
        checkCloseTo("WHT 90,000", tax.calculate(90000.00), 17832.90);
    }

    // ------------------------------------------------------------------ I

    /**
     * Orchestrator acceptance (two-phase loop, period overload, non-positive gross): proves the ORCHESTRATOR, not the individual calculators.
     *
     * Path under test: attendance days -> gross (workedDays * hourlyRate * 8) -> phase 1 statutory
     * loop over List<GovernmentContributionCalculator> -> phase 2 tax on the taxable income -> net
     * -> one immutable PayrollRecord. The same method is called with two different concrete
     * employee types through the same Employee reference, and a negative payslip can no longer be created from
     * negative payslip because a non-positive gross is rejected before any deduction runs.
     */
    private static void testPayrollCalculatorOrchestrator() {
        AttendanceLog log = new AttendanceLog();
        int regularId = 10001;
        int probationaryId = 10034;
        for (int day = 3; day <= 22; day++) {
            String date = String.format(Locale.ROOT, "2024-06-%02d", day);
            for (int employeeId : new int[]{regularId, probationaryId}) {
                log.recordClockIn(employeeId, date, "08:00:00");
                log.recordClockOut(employeeId, date, "17:00:00");
            }
        }

        PayrollCalculator calculator = new PayrollCalculator();
        System.out.println("  " + calculator);

        Employee regular = new RegularEmployee(regularId, "Pore", "Paul", "1990-02-14", "Manila",
                "09171234567", "34-1234567-8", "12-345678901-2", "123-456-789-000", "1234-5678-9012",
                "Regular", "Software Engineer", "Valencia", 35000.00, 1500, 2000, 1000, 17500, 205.36, 3);
        Employee probationary = new ProbationaryEmployee(probationaryId, "Racimo", "Ana", "1998-07-01",
                "Quezon City", "09181234567", "34-9876543-2", "12-987654321-9", "987-654-321-000",
                "9876-5432-1098", "Probationary", "Junior Developer", "Pore", 22000.00, 1000, 500, 800,
                11000, 129.08, 5, "2026-06-01", 88.5);

        PayrollRecord record = calculator.calculatePayroll(regular, log, "2024-06-01", "2024-06-30");
        System.out.println("  " + record);
        System.out.println("    hoursWorked = " + money(record.getHoursWorked())
                + "  totalDeductions = " + money(record.getTotalDeductions()));

        // gross = 20 worked days * 205.36 hourly rate * 8 - the same formula CP2 double-implemented
        checkCloseTo("gross = 20 days * 205.36 * 8 (single implementation now)", record.getGrossIncome(), 32857.60);
        checkCloseTo("hoursWorked = 20 days * 8", record.getHoursWorked(), 160.00);

        // phase 1 - each statutory amount comes from the official matrix, not from CP2
        checkCloseTo("phase 1 SSS at the official cap", record.getSSSContribution(), 1125.00);
        checkCloseTo("phase 1 PhilHealth employee share (985.728 premium / 2)",
                record.getPhilHealthContribution(), 492.864);
        checkCloseTo("phase 1 Pag-IBIG employee share at the official cap", record.getPagIbigContribution(), 100.00);

        // phase 2 - tax runs on the income already net of the three statutory deductions
        double taxableIncome = 32857.60 - 1125.00 - 492.864 - 100.00;
        checkCloseTo("phase 2 withholding tax on the taxable income (not on gross)",
                record.getWithholdingTax(), (taxableIncome - 20833.00) * 0.20);
        checkCloseTo("net = gross - (sss + philhealth + pagibig + tax)",
                record.getNetIncome(), 32857.60 - record.getTotalDeductions());
        checkCloseTo("getTotalDeductions() = the four amounts",
                record.getTotalDeductions(),
                record.getSSSContribution() + record.getPhilHealthContribution()
                        + record.getPagIbigContribution() + record.getWithholdingTax());

        // immutability - the result cannot be edited after the computation
        boolean setterFound = false;
        for (Method method : PayrollRecord.class.getDeclaredMethods()) {
            if (method.getName().startsWith("set")) {
                setterFound = true;
            }
        }
        check("the returned PayrollRecord has no setters (immutable result)", !setterFound);

        // polymorphism - same call, two types, no if/else on employee type anywhere in the flow
        PayrollRecord probationaryRecord =
                calculator.calculatePayroll(probationary, log, "2024-06-01", "2024-06-30");
        check("two employee types through ONE Employee parameter give different benefits",
                record.getRiceSubsidy() == 1500.0 && probationaryRecord.getRiceSubsidy() == 1000.0);
        check("each record is priced with its own hourly rate (205.36 vs 129.08)",
                Math.abs(probationaryRecord.getGrossIncome() - 20 * 129.08 * 8) < 0.005);

        // composition - the calculator list is injectable, so a payroll variant needs no edit here
        List<GovernmentContributionCalculator> sssOnly = new ArrayList<>();
        sssOnly.add(new SSSCalculator());
        PayrollRecord sssOnlyRecord = new PayrollCalculator(sssOnly, new WithholdingTaxCalculator())
                .calculatePayroll(regular, log, "2024-06-01", "2024-06-30");
        check("injected calculator list changes only the amounts it contains (SSS only)",
                sssOnlyRecord.getSSSContribution() == record.getSSSContribution()
                        && sssOnlyRecord.getPhilHealthContribution() == 0.0
                        && sssOnlyRecord.getPagIbigContribution() == 0.0);

        // Issue 1 - a non-positive gross is rejected instead of producing a payslip
        AttendanceLog emptyLog = new AttendanceLog();
        expectThrows("zero attendance is rejected (no -285.00 payslip)",
                () -> calculator.calculatePayroll(regular, emptyLog, "2024-06-01", "2024-06-30"));
        expectThrows("periodStart after periodEnd rejected",
                () -> calculator.calculatePayroll(regular, log, "2024-06-30", "2024-06-01"));
        expectThrows("malformed period date rejected",
                () -> calculator.calculatePayroll(regular, log, "06/01/2024", "2024-06-30"));
        expectThrows("null attendance rejected", () -> calculator.calculatePayroll(regular, null));
        expectThrows("empty calculator list rejected", () -> new PayrollCalculator(new ArrayList<>(),
                new WithholdingTaxCalculator()));

        // Overload without dates - uses the most recent semi-monthly period
        String today = LocalDate.now().toString();
        AttendanceLog todayLog = new AttendanceLog();
        todayLog.recordClockIn(regularId, today, "08:00:00");
        todayLog.recordClockOut(regularId, today, "17:00:00");
        PayrollRecord defaultPeriodRecord = calculator.calculatePayroll(regular, todayLog);
        checkCloseTo("overload without dates prices today's 8 hours",
                defaultPeriodRecord.getGrossIncome(), 205.36 * 8);
        check("the default period ends today",
                defaultPeriodRecord.getPeriodEnd().toLocalDate().toString().equals(today));
    }

    // ------------------------------------------------------------- helpers

    private static void checkCloseTo(String description, double actual, double expected) {
        boolean ok = Math.abs(actual - expected) <= 0.005;
        if (ok) {
            passed++;
            System.out.println("  PASS  " + description + "  = " + money(actual));
        } else {
            failed++;
            System.out.println("  FAIL  " + description + "  expected " + money(expected)
                    + " but was " + money(actual));
        }
    }

    private static Employee sampleRegular() {
        return new RegularEmployee(10001, "Pore", "Paul", "1990-02-14", "Manila", "09171234567",
                "34-1234567-8", "12-345678901-2", "123-456-789-000", "1234-5678-9012",
                "Regular", "Software Engineer", "Valencia", 35000.00, 1500, 2000, 1000, 17500, 12.0, 3);
    }

    private static void section(String title) {
        System.out.println("--- " + title);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + description);
        } else {
            failed++;
            System.out.println("  FAIL  " + description);
        }
    }

    private static void expectThrows(String description, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL  " + description + "  (no exception thrown)");
        } catch (RuntimeException expected) {
            passed++;
            System.out.println("  PASS  " + description + "  -> " + expected.getClass().getSimpleName()
                    + ": " + expected.getMessage());
        }
    }

    private static String money(double value) {
        return String.format(Locale.ROOT, "%,.2f", value);
    }
}
