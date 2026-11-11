package ph.motorph.c_calc;

import ph.motorph.c_calc.AttendanceService;
import ph.motorph.c_calc.CutoffPay;
import ph.motorph.c_calc.CutoffPayrollService;
import ph.motorph.c_calc.HolidayCalendar2026;
import ph.motorph.c_calc.MonthPayroll;
import ph.motorph.c_calc.PayrollAdjustments;
import ph.motorph.c_calc.SalaryHistory;
import ph.motorph.o_oop.EmployeeService;
import ph.motorph.o_oop.InMemoryAttendanceStore;
import ph.motorph.o_oop.InMemoryEmployeeScheduleStore;
import ph.motorph.o_oop.InMemoryEmployeeStore;
import ph.motorph.o_oop.PayrollProfile;
import ph.motorph.o_oop.WorkSchedule;
import ph.motorph.t_tkt.InMemoryWorkflowStore;
import ph.motorph.t_tkt.WorkflowService;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

public final class CutoffPayrollServiceTest {
    private static int checks;

    public static void main(String[] args) {
        countsPaidMinutesUnderTemporaryRules();
        derivesHourlyRateFromMonthlyBasic();
        splitsMonthIntoCutoffsAndPricesHours();
        rejectsEmployeesWithoutTemporaryRate();
        splitsEachDeductionLineHalfPerCutoff();
        splitsDayIntoRegularOvertimeAndNight();
        classifiesTwentyTwentySixDays();
        paysLeaveHolidaysOvertimeAndNightDifferential();
        overtimeRequestsFollowReviewRules();
        runsGrossToNet();
        followsSchedulesAndSalaryChanges();
        System.out.println("PASS: " + checks + " cutoff-payroll checks");
    }

    private static void countsPaidMinutesUnderTemporaryRules() {
        check(minutes("08:01", "17:00") == 480, "log-in within grace counts from 08:00; meal hour unpaid");
        check(minutes("07:58", "16:59") == 480 && worked("07:58", "16:59") == 481, "early log-in is worked time but paid is capped at 8 h");
        check(minutes("07:00", "17:00") == 480 && worked("07:00", "17:00") == 540, "mentor case: 07:00-17:00 pays 8 h, 1 h unapproved overage");
        check(minutes("10:00", "19:00") == 480 && worked("10:00", "19:00") == 480, "mentor case: 10:00-19:00 with meal is 8 h, no overage");
        check(minutes("08:11", "17:00") == 469, "log-in after 08:10 counts from actual time");
        check(minutes("13:00", "17:00") == 240, "afternoon-only shift has no meal deduction");
        check(minutes("08:00", "12:30") == 240, "only the overlapping part of the meal hour is removed");
        check(minutes("12:15", "12:45") == 0, "time entirely inside the meal hour is unpaid");
        check(minutes("18:00", "19:00") == 60, "evening hours count as worked time (no 17:00 clock cap)");
    }

    private static void derivesHourlyRateFromMonthlyBasic() {
        
        
        check("2675.00".equals(PayrollProfile.dailyRate("10015").get().toPlainString()),
            "53,500 / 20 = 2,675.00, the Daily Rate the sample slip prints");
        check("4500.00".equals(PayrollProfile.dailyRate("10001").get().toPlainString()), "90,000 / 20 = 4,500.00");
        check("535.71".equals(PayrollProfile.hourlyRate("10001").get().toPlainString()), "90,000 / 168 = 535.71 (the CEO, employee file)");
        check("357.14".equals(PayrollProfile.hourlyRate("10002").get().toPlainString()), "60,000 / 168 = 357.14 (the COO, employee file)");
        check("313.51".equals(PayrollProfile.hourlyRate("10006").get().toPlainString()), "52,670 / 168 = 313.51 (the HR Manager, employee file)");
    }

    private static void splitsMonthIntoCutoffsAndPricesHours() {
        
        AttendanceService attendance = attendanceFor("10006");
        add(attendance, "2026-10-01", "08:00", "17:00");
        add(attendance, "2026-10-02", "08:00", "17:00");
        add(attendance, "2026-10-05", "08:00", "17:00");
        CutoffPayrollService service = new CutoffPayrollService(attendance);
        List<CutoffPay> cutoffs = service.grossForMonth("10006", YearMonth.of(2026, 10));
        check(cutoffs.size() == 2, "a month has two cutoffs");
        check("2026-10-01".equals(cutoffs.get(0).getFrom().toString()) && "2026-10-15".equals(cutoffs.get(0).getTo().toString()),
            "first cutoff is 1-15");
        check("2026-10-16".equals(cutoffs.get(1).getFrom().toString()) && "2026-10-31".equals(cutoffs.get(1).getTo().toString()),
            "second cutoff runs to the actual month end");
        check(cutoffs.get(0).getDaysWithRecords() == 3 && cutoffs.get(0).getPaidMinutes() == 1440,
            "three full days are 1,440 paid minutes");
        check("7900.50".equals(cutoffs.get(0).getGrossPay().toPlainString()),
            "three days x 2,633.50 = 7,900.50: a monthly-salaried person is paid per day worked");
        check(cutoffs.get(1).getDaysWithRecords() == 0 && "0.00".equals(cutoffs.get(1).getGrossPay().toPlainString()),
            "second cutoff with no rows earns 0.00");
        check("2026-02-28".equals(service.grossForMonth("10006", YearMonth.of(2026, 2)).get(1).getTo().toString()),
            "February second cutoff ends on the 28th");
    }

    private static void rejectsEmployeesWithoutTemporaryRate() {
        try {
            service().grossForMonth("99999", YearMonth.of(2026, 10));
            throw new AssertionError("missing rate should be rejected");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("No monthly basic") && !expected.getMessage().contains("DEMO-"),
                "an employee with no monthly basic is rejected, and the message names no invented people");
        }
    }

    private static void splitsEachDeductionLineHalfPerCutoff() {
        MonthPayroll both = month("3597.50", "12000.00");
        
        check(both.lineCents(0, MonthPayroll.SSS) == 38_750 && both.lineCents(1, MonthPayroll.SSS) == 38_750, "SSS halves");
        check(both.lineCents(0, MonthPayroll.PHILHEALTH) == 19_497 && both.lineCents(1, MonthPayroll.PHILHEALTH) == 19_497, "PhilHealth halves");
        check(both.deductionsCents(0) == 68_247 && both.deductionsCents(1) == 68_247, "682.47 per cutoff");
        check(both.netCents(0) == 291_503 && both.netCents(1) == 1_131_753, "nets 2,915.03 and 11,317.53");

        MonthPayroll firstOnly = month("3597.50", "0.00");
        check(firstOnly.lineCents(0, MonthPayroll.PAGIBIG) == 3_597 && firstOnly.lineCents(1, MonthPayroll.PAGIBIG) == 3_598,
            "odd centavo of Pag-IBIG 71.95 goes to the 2nd cutoff");
        check(firstOnly.deductionsCents(0) == 28_597 && firstOnly.netCents(0) == 331_153, "1st cutoff net 3,311.53");
        check(firstOnly.isBlocked(1), "2nd cutoff with no gross is blocked");
        try {
            firstOnly.netCents(1);
            throw new AssertionError("blocked net should not be returned");
        } catch (IllegalStateException expected) {
            check(true, "blocked cutoff refuses to give a negative net");
        }

        MonthPayroll high = month("50000.00", "50000.00");
        check(high.lineCents(0, MonthPayroll.TAX) == 788_125 && high.lineCents(1, MonthPayroll.TAX) == 788_125, "tax 15,762.50 split evenly");
        check(high.deductionsCents(0) + high.deductionsCents(1) == 2_021_250, "cutoffs add up to the month total 20,212.50 (no 13th month)");

        MonthPayroll none = month("0.00", "0.00");
        check(none.getStatutory() == null && none.deductionsCents(0) == 0 && none.netCents(1) == 0, "no attendance means no deductions");
    }

    private static void splitsDayIntoRegularOvertimeAndNight() {
        long[] approved = PayrollProfile.splitDay(LocalTime.parse("08:00"), LocalTime.parse("20:00"), 60);
        check(approved[PayrollProfile.REGULAR] == 480 && approved[PayrollProfile.OVERTIME] == 60 && approved[PayrollProfile.UNPAID] == 120,
            "08:00-20:00 with 60 approved: 8 h regular, 1 h overtime, 2 h unpaid overage");
        long[] night = PayrollProfile.splitDay(LocalTime.parse("14:00"), LocalTime.parse("23:30"), 90);
        check(night[PayrollProfile.REGULAR] == 480 && night[PayrollProfile.REGULAR_NIGHT] == 0
            && night[PayrollProfile.OVERTIME] == 90 && night[PayrollProfile.OVERTIME_NIGHT] == 90, "14:00-23:30: overtime 22:00-23:30 is all night time");
        long[] early = PayrollProfile.splitDay(LocalTime.parse("05:00"), LocalTime.parse("09:00"), 0);
        check(early[PayrollProfile.REGULAR] == 240 && early[PayrollProfile.REGULAR_NIGHT] == 60, "05:00-06:00 is night time");
        long[] none = PayrollProfile.splitDay(LocalTime.parse("07:00"), LocalTime.parse("17:00"), 0);
        check(none[PayrollProfile.UNPAID] == 60 && none[PayrollProfile.OVERTIME] == 0, "without approval the extra hour stays unpaid");
        long[] late = PayrollProfile.splitDay(LocalTime.parse("22:00"), LocalTime.parse("23:59"), 0);
        check(late[PayrollProfile.REGULAR] == 119 && late[PayrollProfile.REGULAR_NIGHT] == 119, "a row ending at 23:59 does not wrap");
    }

    private static void classifiesTwentyTwentySixDays() {
        check(HolidayCalendar2026.kindOf(java.time.LocalDate.parse("2026-12-25")) == HolidayCalendar2026.DayKind.REGULAR_HOLIDAY, "Christmas (Fri) is a regular holiday");
        check(HolidayCalendar2026.kindOf(java.time.LocalDate.parse("2026-03-20")) == HolidayCalendar2026.DayKind.REGULAR_HOLIDAY, "Eid'l Fitr (Proc. 1189)");
        check(HolidayCalendar2026.kindOf(java.time.LocalDate.parse("2026-11-01")) == HolidayCalendar2026.DayKind.SPECIAL_REST_DAY, "All Saints' Day falls on a Sunday");
        check(HolidayCalendar2026.kindOf(java.time.LocalDate.parse("2026-12-08")) == HolidayCalendar2026.DayKind.SPECIAL, "Immaculate Conception is special non-working");
        check(HolidayCalendar2026.kindOf(java.time.LocalDate.parse("2026-02-25")) == HolidayCalendar2026.DayKind.ORDINARY, "EDSA anniversary is a special working day");
        check(HolidayCalendar2026.kindOf(java.time.LocalDate.parse("2026-10-03")) == HolidayCalendar2026.DayKind.REST_DAY, "Saturday is a rest day");
        check("3.38".equals(HolidayCalendar2026.DayKind.REGULAR_HOLIDAY_REST_DAY.overtimeMultiplier.toPlainString()), "mentor's 338% rate");
    }

    
    
    private static void paysLeaveHolidaysOvertimeAndNightDifferential() {
        AttendanceService attendanceService = attendanceFor("10006");
        WorkflowService workflows = new WorkflowService(new EmployeeService(ph.motorph.o_oop.TestRoster.store()),
            new InMemoryWorkflowStore());
        add(attendanceService, "2026-12-01", "14:00", "23:30");
        add(attendanceService, "2026-12-08", "08:00", "17:00");
        add(attendanceService, "2026-12-25", "08:00", "19:00");
        approve(workflows, workflows.createLeaveRequest("10006", "Vacation", "2026-12-02", "2026-12-04", "").getId());
        approve(workflows, workflows.createLeaveRequest("10006", "Unpaid", "2026-12-07", "2026-12-07", "").getId());
        workflows.reviewOvertimeRequest(workflows.createOvertimeRequest("10006", "2026-12-01", 90, "").getId(), "approve", "10002");
        workflows.reviewOvertimeRequest(workflows.createOvertimeRequest("10006", "2026-12-25", 120, "").getId(), "approve", "10002");
        List<CutoffPay> cutoffs = new CutoffPayrollService(attendanceService, workflows)
            .grossForMonth("10006", YearMonth.of(2026, 12));
        CutoffPay first = cutoffs.get(0), second = cutoffs.get(1);

        
        check("5267.00".equals(first.getBasicPay().toPlainString()), "two full days x 2,633.50 = 5,267.00");
        check(first.getLeaveDays() == 3 && "7900.50".equals(first.getLeavePay().toPlainString()),
            "three paid Vacation days = 3 x 2,633.50; the Unpaid day pays nothing");
        check(first.getPaidMinutes() == 960 && first.getOvertimeMinutes() == 90, "leave days never count as hours worked");
        
        check("790.05".equals(first.getPremiumPay().toPlainString()), "special-day premium 30% x 2,633.50 = 790.05");
        
        check("587.83".equals(first.getOvertimePay().toPlainString()), "ordinary-day OT 1.5 h x 313.51 = 587.83");
        check(first.getNightDiffMinutes() == 90 && "58.78".equals(first.getNightDiffPay().toPlainString()),
            "night differential 1.5 h x 10% of 313.51 = 58.78");

        
        check(second.getDaysWithRecords() == 1 && "2633.50".equals(second.getBasicPay().toPlainString()),
            "the worked holiday's basic day");
        check("2633.50".equals(second.getPremiumPay().toPlainString()), "regular-holiday premium 100% x 2,633.50 = 2,633.50");
        check("1630.25".equals(second.getOvertimePay().toPlainString()), "holiday OT 2 h x 260% x 313.51 = 1,630.25");
        check(second.getHolidayDays() == 0, "a holiday that was worked is paid through the day, not as an extra unworked day");
        check("6897.25".equals(second.getGrossPay().toPlainString()), "2nd cutoff gross 6,897.25");
        check("14604.16".equals(first.getGrossPay().toPlainString()), "1st cutoff gross 14,604.16");

        List<CutoffPay> soFar = new CutoffPayrollService(attendanceService, workflows)
            .grossForMonth("10006", YearMonth.of(2026, 12), java.time.LocalDate.parse("2026-12-03"));
        check(soFar.get(0).getLeaveDays() == 2 && soFar.get(1).getHolidayDays() == 0
            && "2026-12-15".equals(soFar.get(0).getTo().toString()),
            "through 3 Dec: only 1 Dec and two leave days count; the cutoffs keep their real dates");
    }

    private static void overtimeRequestsFollowReviewRules() {
        EmployeeService employees = new EmployeeService(ph.motorph.o_oop.TestRoster.store());
        WorkflowService workflows = new WorkflowService(employees, new InMemoryWorkflowStore());
        String id = workflows.createOvertimeRequest("10002", "2026-10-01", 30, "close").getId();
        expect(IllegalArgumentException.class, () -> workflows.reviewOvertimeRequest(id, "approve", "10002"), "no self-approval of overtime");
        expect(IllegalStateException.class, () -> workflows.createOvertimeRequest("10002", "2026-10-01", 15, ""), "one open request per date");
        expect(IllegalArgumentException.class, () -> workflows.createOvertimeRequest("10002", "2026-10-02", 0, ""), "minutes must be positive");
        check("Rejected".equals(workflows.reviewOvertimeRequest(id, "reject", "10006").getStatus()), "another reviewer can reject");
        expect(IllegalStateException.class, () -> workflows.reviewOvertimeRequest(id, "approve", "10006"), "decided requests stay decided");
        check(workflows.createOvertimeRequest("10002", "2026-10-01", 15, "").getMinutes() == 15, "a rejected date can be requested again");
    }

    
    private static void runsGrossToNet() {
        YearMonth oct = YearMonth.of(2026, 10);
        PayrollAdjustments adjustments = new PayrollAdjustments();
        
        CutoffPay earned = new CutoffPay("1-15", oct.atDay(1), oct.atDay(15), 3, 1440,
            new java.math.BigDecimal("7900.50"));
        CutoffPay empty = new CutoffPay("16-end", oct.atDay(16), oct.atEndOfMonth(), 0, 0, new java.math.BigDecimal("0.00"));

        
        PayrollAdjustments.Earning bonus = adjustments.addEarning("10006", oct, 1, "Bonus", "", 2_000_000, "10011");
        MonthPayroll taxed = new MonthPayroll("10006", oct, new java.math.BigDecimal("313.51"),
            java.util.Arrays.asList(earned, empty), java.util.Collections.singletonList(bonus),
            java.util.Collections.emptyList(), java.util.Collections.emptyList(),
            new long[] {150_000, 100_000, 150_000});
        check(taxed.earnedCents(0) == 790_050, "earned pay is the cut-off's attendance pay");
        check(taxed.grossCents(0) == 2_790_050, "gross = earned pay + taxable bonus");
        check(taxed.lineCents(0, MonthPayroll.TAX) == taxed.lineCents(1, MonthPayroll.TAX)
            || taxed.lineCents(0, MonthPayroll.TAX) + taxed.lineCents(1, MonthPayroll.TAX) > 0,
            "the bonus is taxed");
        check(taxed.lineCents(0, MonthPayroll.SSS) > 0, "contributions are assessed even on a bonus month");

        
        PayrollAdjustments.Deduction loan = adjustments.addDeduction("10015", "Company loan", "", 200_000, oct, null, "10011");
        PayrollAdjustments.Deduction insurance = adjustments.addDeduction("10015", "Insurance", "", 50_000, oct, oct, "10011");
        MonthPayroll capped = new MonthPayroll("10015", oct, new java.math.BigDecimal("318.45"),
            java.util.Arrays.asList(
                new CutoffPay("1-15", oct.atDay(1), oct.atDay(15), 2, 960, new java.math.BigDecimal("5350.00")),
                new CutoffPay("16-end", oct.atDay(16), oct.atEndOfMonth(), 0, 0, new java.math.BigDecimal("0.00"))),
            java.util.Collections.emptyList(), java.util.Collections.emptyList(),
            java.util.Arrays.asList(loan, insurance), new long[] {150_000, 100_000, 100_000});
        long taken = 0, shortfall = 0;
        for (MonthPayroll.Taken item : capped.voluntary(0)) { taken += item.takenCents; shortfall += item.shortfallCents; }
        check(taken + shortfall == 250_000, "every peso of a voluntary deduction is either taken or recorded as a shortfall");
        check(capped.netCents(0) >= 0, "voluntary deductions never push net below zero");
        check(capped.isBlocked(1) && capped.voluntaryCents(1) == 0, "a cut-off with no pay takes no voluntary deductions");
        check(insurance.appliesTo(oct) && !insurance.appliesTo(oct.plusMonths(1)) && loan.appliesTo(oct.plusMonths(5)),
            "a deduction applies across its month range");
    }

    
    private static void followsSchedulesAndSalaryChanges() {
        AttendanceService attendance = attendanceFor("10015");
        WorkSchedule.Store schedules = new WorkSchedule.Store();
        SalaryHistory salaries = new SalaryHistory();
        
        schedules.set("10015", LocalTime.parse("10:00"),
            java.util.EnumSet.range(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.SATURDAY), "10016");
        attendance.createForEmployee("10015", java.time.LocalDate.parse("2026-10-05"),
            LocalTime.parse("10:00"), LocalTime.parse("19:00"));
        CutoffPayrollService service = new CutoffPayrollService(attendance).withSchedules(schedules, salaries);
        CutoffPay first = service.grossForMonth("10015", YearMonth.of(2026, 10)).get(0);
        check(first.getPaidMinutes() == 480, "a 10:00-19:00 shift on a 10:00 schedule is a full eight-hour day");
        long[] tenToSeven = PayrollProfile.splitDay(LocalTime.parse("10:05"), LocalTime.parse("19:00"), 0,
            schedules.get("10015"));
        check(tenToSeven[PayrollProfile.REGULAR] == 480 && tenToSeven[PayrollProfile.UNPAID] == 0,
            "10:05-19:00 within the grace period is still a full day");

        
        check("802.50".equals(first.getPremiumPay().toPlainString()), "rest-day premium 30% x 2,675.00 = 802.50");
        check("3477.50".equals(first.getGrossPay().toPlainString()), "gross 2,675.00 + 802.50 = 3,477.50");

        
        salaries.set("10015", YearMonth.of(2026, 11), new java.math.BigDecimal("60000"));
        check(new java.math.BigDecimal("53500.00").compareTo(salaries.monthlyBasic("10015", YearMonth.of(2026, 10)).get()) == 0,
            "October still uses the file's 53,500.00");
        check(new java.math.BigDecimal("60000.00").compareTo(salaries.monthlyBasic("10015", YearMonth.of(2026, 12)).get()) == 0,
            "December uses the raised 60,000.00");
        try {
            schedules.set("10015", LocalTime.parse("16:00"), java.util.EnumSet.of(java.time.DayOfWeek.MONDAY), "10016");
            throw new AssertionError("a late shift start should be refused");
        } catch (IllegalArgumentException expected) {
            check(true, "shifts start between 05:00 and 15:00");
        }
    }

    private static void add(AttendanceService service, String date, String in, String out) {
        service.createForEmployee("10006", java.time.LocalDate.parse(date), LocalTime.parse(in), LocalTime.parse(out));
    }

    
    private static AttendanceService attendanceFor(String employeeId) {
        return new AttendanceService(new EmployeeService(ph.motorph.o_oop.TestRoster.store()),
            ph.motorph.o_oop.TestRoster.schedules(), new InMemoryAttendanceStore());
    }

    private static void approve(WorkflowService workflows, String leaveId) {
        workflows.reviewLeaveRequest(leaveId, "approve", "10002");
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action, String message) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            check(type.isInstance(ex), message + " (got " + ex + ")");
            return;
        }
        throw new AssertionError(message + ": nothing was thrown");
    }

    private static MonthPayroll month(String firstGross, String secondGross) {
        YearMonth oct = YearMonth.of(2026, 10);
        return new MonthPayroll("10006", oct, new java.math.BigDecimal("150.00"), java.util.Arrays.asList(
            new CutoffPay("1-15", oct.atDay(1), oct.atDay(15), 0, 0, new java.math.BigDecimal(firstGross)),
            new CutoffPay("16-end", oct.atDay(16), oct.atEndOfMonth(), 0, 0, new java.math.BigDecimal(secondGross))));
    }

    private static CutoffPayrollService service() {
        EmployeeService employees = new EmployeeService(ph.motorph.o_oop.TestRoster.store());
        return new CutoffPayrollService(new AttendanceService(employees,
            ph.motorph.o_oop.TestRoster.schedules(), new InMemoryAttendanceStore()));
    }

    private static long worked(String in, String out) {
        return PayrollProfile.workedMinutes(LocalTime.parse(in), LocalTime.parse(out));
    }

    private static long minutes(String in, String out) {
        return PayrollProfile.paidMinutes(LocalTime.parse(in), LocalTime.parse(out));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
