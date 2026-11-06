package ph.motorph.web;

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/** Hand-worked checks for the temporary attendance-to-gross payroll profile. */
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
        check("535.71".equals(PayrollProfile.hourlyRate("DEMO-003").get().toPlainString()), "90,000 / 168 = 535.71 (employee file)");
        check("357.14".equals(PayrollProfile.hourlyRate("DEMO-002").get().toPlainString()), "60,000 / 168 = 357.14 (employee file)");
        check("313.51".equals(PayrollProfile.hourlyRate("DEMO-001").get().toPlainString()), "52,670 / 168 = 313.51");
    }

    private static void splitsMonthIntoCutoffsAndPricesHours() {
        CutoffPayrollService service = service();
        List<CutoffPay> mara = service.grossForMonth("DEMO-001", YearMonth.of(2026, 10));
        check(mara.size() == 2, "a month has two cutoffs");
        check("2026-10-01".equals(mara.get(0).getFrom().toString()) && "2026-10-15".equals(mara.get(0).getTo().toString()),
            "first cutoff is 1-15");
        check("2026-10-16".equals(mara.get(1).getFrom().toString()) && "2026-10-31".equals(mara.get(1).getTo().toString()),
            "second cutoff runs to the actual month end");
        check(mara.get(0).getDaysWithRecords() == 3 && mara.get(0).getPaidMinutes() == 1440
            && mara.get(0).getUnpaidOverageMinutes() == 2, "DEMO-001 first cutoff: 3 rows, 1440 paid minutes, 2 unpaid overage");
        check("7524.24".equals(mara.get(0).getGrossPay().toPlainString()), "24 h x 313.51 = 7524.24");
        check(mara.get(1).getDaysWithRecords() == 0 && "0.00".equals(mara.get(1).getGrossPay().toPlainString()),
            "second cutoff with no rows earns 0.00");
        List<CutoffPay> eli = service.grossForMonth("DEMO-002", YearMonth.of(2026, 10));
        check(eli.get(0).getPaidMinutes() == 960 && eli.get(0).getUnpaidOverageMinutes() == 5
            && "5714.24".equals(eli.get(0).getBasicPay().toPlainString()), "DEMO-002: 16 h x 357.14 = 5714.24 basic, 5 min overage");
        // 2026-10-03 is a Saturday (rest day): 8 h x 30% premium = 2.4 h x 357.14 = 857.136 -> 857.14
        check("857.14".equals(eli.get(0).getPremiumPay().toPlainString()) && "6571.38".equals(eli.get(0).getGrossPay().toPlainString()),
            "DEMO-002 Saturday row earns the 30% rest-day premium");
        check("2026-02-28".equals(service.grossForMonth("DEMO-001", YearMonth.of(2026, 2)).get(1).getTo().toString()),
            "February second cutoff ends on the 28th");
    }

    private static void rejectsEmployeesWithoutTemporaryRate() {
        try {
            service().grossForMonth("DEMO-999", YearMonth.of(2026, 10));
            throw new AssertionError("missing rate should be rejected");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("No temporary monthly basic"), "employee without a monthly basic is rejected");
        }
    }

    private static void splitsEachDeductionLineHalfPerCutoff() {
        MonthPayroll both = month("3597.50", "12000.00");
        // monthly 15,597.50 (sweldongpinoy.com): SSS 775.00, PhilHealth 389.94, Pag-IBIG 200.00, tax 0
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

    /**
     * DEMO-001 (313.51/h), December 2026, hand-worked:
     * Dec 1 (Tue) 14:00-23:30, 90 OT approved: basic 480, OT 90 x 1.25 = 112.5, night 90 x 1.25 x 10% = 11.25 weighted minutes
     * Dec 2-4 (Wed-Fri) approved Vacation: 3 leave days; Dec 7 (Mon) Unpaid leave: nothing
     * Dec 8 (Tue, special) 08:00-17:00: basic 480, premium 480 x 30% = 144
     * Dec 25 (Fri, regular) 08:00-19:00, 120 OT approved: basic 480, premium 480 x 100%, OT 120 x 2.60 = 312
     * Dec 30 (Wed, regular) not worked: 1 holiday day; Dec 24 and 31 (special) not worked: nothing
     */
    private static void paysLeaveHolidaysOvertimeAndNightDifferential() {
        EmployeeService employees = new EmployeeService(new InMemoryEmployeeStore());
        InMemoryAttendanceStore attendance = new InMemoryAttendanceStore();
        AttendanceService attendanceService = new AttendanceService(employees, new InMemoryEmployeeScheduleStore(), attendance);
        WorkflowService workflows = new WorkflowService(employees, new InMemoryWorkflowStore());
        add(attendanceService, "2026-12-01", "14:00", "23:30");
        add(attendanceService, "2026-12-08", "08:00", "17:00");
        add(attendanceService, "2026-12-25", "08:00", "19:00");
        approve(workflows, workflows.createLeaveRequest("DEMO-001", "Vacation", "2026-12-02", "2026-12-04", "").getId());
        approve(workflows, workflows.createLeaveRequest("DEMO-001", "Unpaid", "2026-12-07", "2026-12-07", "").getId());
        workflows.reviewOvertimeRequest(workflows.createOvertimeRequest("DEMO-001", "2026-12-01", 90, "").getId(), "approve", "DEMO-002");
        workflows.reviewOvertimeRequest(workflows.createOvertimeRequest("DEMO-001", "2026-12-25", 120, "").getId(), "approve", "DEMO-002");
        List<CutoffPay> dec = new CutoffPayrollService(attendanceService, workflows).grossForMonth("DEMO-001", YearMonth.of(2026, 12));
        CutoffPay first = dec.get(0), second = dec.get(1);
        check("5016.16".equals(first.getBasicPay().toPlainString()), "16 h basic = 5016.16");
        check("752.42".equals(first.getPremiumPay().toPlainString()), "special-day premium 2.4 h x 313.51 = 752.42");
        check("587.83".equals(first.getOvertimePay().toPlainString()), "ordinary-day OT 1.875 h x 313.51 = 587.83");
        check("58.78".equals(first.getNightDiffPay().toPlainString()) && first.getNightDiffMinutes() == 90, "night diff 0.1875 h x 313.51 = 58.78");
        check(first.getLeaveDays() == 3 && "7524.24".equals(first.getLeavePay().toPlainString()), "3 paid Vacation days = 24 h = 7524.24; Unpaid leave pays nothing");
        check(first.getPaidMinutes() == 960 && first.getOvertimeMinutes() == 90, "leave days never count as hours worked");
        check("13939.43".equals(first.getGrossPay().toPlainString()), "1st cutoff gross 13,939.43");
        check("2508.08".equals(second.getPremiumPay().toPlainString()), "regular-holiday premium 8 h x 100%");
        check("1630.25".equals(second.getOvertimePay().toPlainString()), "regular-holiday OT 5.2 h x 313.51 = 1630.25");
        check(second.getHolidayDays() == 1 && "2508.08".equals(second.getHolidayPay().toPlainString()), "unworked Rizal Day paid 8 h; unworked special days not paid");
        check("9154.49".equals(second.getGrossPay().toPlainString()), "2nd cutoff gross 9,154.49");
        List<CutoffPay> soFar = new CutoffPayrollService(attendanceService, workflows).grossForMonth("DEMO-001", YearMonth.of(2026, 12), java.time.LocalDate.parse("2026-12-03"));
        check(soFar.get(0).getLeaveDays() == 2 && soFar.get(0).getPaidMinutes() == 480 && soFar.get(1).getHolidayDays() == 0
            && "2026-12-15".equals(soFar.get(0).getTo().toString()), "through 3 Dec: only Dec 1 work and 2 leave days count; cutoffs keep their real dates");
    }

    /**
     * Jae (148.81/h) moves to a 10:00 shift, Tuesday-Saturday. October 1-15 seed rows:
     * Mon 5 Oct (now a rest day) 08:00-17:00: not in the 10:00-10:10 grace, meal 14:00-15:00 -> 480 min at 130%;
     * Tue 6 Oct 08:06-17:00 -> 534 - 60 = 474 min. Basic 954 min = 2,366.08; premium 480 x 30% = 144 min = 357.14.
     */
    private static void followsSchedulesAndSalaryChanges() {
        EmployeeService employees = new EmployeeService(new InMemoryEmployeeStore());
        AttendanceService attendance = new AttendanceService(employees, new InMemoryEmployeeScheduleStore(), new InMemoryAttendanceStore());
        WorkSchedule.Store schedules = new WorkSchedule.Store();
        SalaryHistory salaries = new SalaryHistory();
        schedules.set("DEMO-004", LocalTime.parse("10:00"), java.util.EnumSet.range(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.SATURDAY), "DEMO-003");
        CutoffPayrollService service = new CutoffPayrollService(attendance).withSchedules(schedules, salaries);
        CutoffPay first = service.grossForMonth("DEMO-004", YearMonth.of(2026, 10)).get(0);
        check(first.getPaidMinutes() == 954 && "2366.08".equals(first.getBasicPay().toPlainString()), "schedule moves the grace and meal hour");
        check("357.14".equals(first.getPremiumPay().toPlainString()), "a Monday is a rest day on a Tuesday-Saturday schedule");
        long[] tenToSeven = PayrollProfile.splitDay(LocalTime.parse("10:05"), LocalTime.parse("19:00"), 0, schedules.get("DEMO-004"));
        check(tenToSeven[PayrollProfile.REGULAR] == 480 && tenToSeven[PayrollProfile.UNPAID] == 0, "10:05-19:00 on a 10:00 shift is a full 8 h day");
        attendance.correctForEmployee("DEMO-004", 5302, LocalTime.parse("10:05"), LocalTime.parse("19:00"));
        check(service.grossForMonth("DEMO-004", YearMonth.of(2026, 10)).get(0).getPaidMinutes() == 960, "a corrected timesheet changes pay");
        try {
            attendance.correctForEmployee("DEMO-001", 5302, LocalTime.parse("08:00"), LocalTime.parse("17:00"));
            throw new AssertionError("another person's row should be refused");
        } catch (java.util.NoSuchElementException expected) {
            check(true, "a correction must name the row's own employee");
        }
        salaries.set("DEMO-004", YearMonth.of(2026, 11), new java.math.BigDecimal("27000"));
        check("148.81".equals(salaries.hourlyRate("DEMO-004", YearMonth.of(2026, 10)).get().toPlainString())
            && "160.71".equals(salaries.hourlyRate("DEMO-004", YearMonth.of(2026, 12)).get().toPlainString()), "a raise applies from its effective month (27,000 / 168 = 160.71)");
        try {
            schedules.set("DEMO-004", LocalTime.parse("16:00"), java.util.EnumSet.of(java.time.DayOfWeek.MONDAY), "DEMO-003");
            throw new AssertionError("late shift start should be refused");
        } catch (IllegalArgumentException expected) {
            check(true, "shifts start between 05:00 and 15:00");
        }
    }

    /** Hand-worked gross-to-net cases on MonthPayroll directly. */
    private static void runsGrossToNet() {
        YearMonth oct = YearMonth.of(2026, 10);
        PayrollAdjustments adjustments = new PayrollAdjustments();
        // DEMO-001 October cutoff 1 earned 7,524.24 + 20,000 bonus: taxable 27,524.24 - SSS 375 - PhilHealth 250 - Pag-IBIG 150.48
        // = 26,748.76 x 12 = 320,985.12 -> 15% of 70,985.12 = 10,647.768 / 12 = 887.31 a month -> 443.65 / 443.66.
        PayrollAdjustments.Earning bonus = adjustments.addEarning("DEMO-001", oct, 1, "Bonus", "", 2_000_000, "DEMO-002");
        MonthPayroll taxed = new MonthPayroll("DEMO-001", oct, new java.math.BigDecimal("313.51"), java.util.Arrays.asList(
            new CutoffPay("1-15", oct.atDay(1), oct.atDay(15), 3, 1440, new java.math.BigDecimal("7524.24")),
            new CutoffPay("16-end", oct.atDay(16), oct.atEndOfMonth(), 0, 0, new java.math.BigDecimal("0.00"))),
            java.util.Collections.singletonList(bonus), java.util.Collections.emptyList(), java.util.Collections.emptyList(), new long[] {150_000, 100_000, 100_001});
        check(taxed.lineCents(0, MonthPayroll.TAX) == 44_365 && taxed.lineCents(1, MonthPayroll.TAX) == 44_366, "a taxable bonus raises withholding: 887.31 a month");
        check(taxed.lineCents(0, MonthPayroll.SSS) == 18_750, "bonuses do not change the SSS base (earned pay 7,524.24 -> MSC 7,500)");
        check(taxed.grossCents(0) == 2_752_424 && taxed.earnedCents(0) == 752_424, "gross = earned + bonus");
        check(taxed.allowanceCents(0, MonthPayroll.CLOTHING) == 50_000 && taxed.allowanceTotalCents(1) == 0,
            "allowances are half per cutoff and only for a cutoff with earned pay");

        // Voluntary deductions never push net below zero: room after statutory is taken in order, the rest is a shortfall.
        PayrollAdjustments.Deduction loan = adjustments.addDeduction("DEMO-004", "Company loan", "", 200_000, oct, null, "DEMO-002");
        PayrollAdjustments.Deduction insurance = adjustments.addDeduction("DEMO-004", "Insurance", "", 50_000, oct, oct, "DEMO-002");
        MonthPayroll capped = new MonthPayroll("DEMO-004", oct, new java.math.BigDecimal("148.81"), java.util.Arrays.asList(
            new CutoffPay("1-15", oct.atDay(1), oct.atDay(15), 2, 960, new java.math.BigDecimal("2380.96")),
            new CutoffPay("16-end", oct.atDay(16), oct.atEndOfMonth(), 0, 0, new java.math.BigDecimal("0.00"))),
            java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Arrays.asList(loan, insurance), new long[] {150_000, 50_000, 50_000});
        // 2,380.96 - statutory 273.81 = 2,107.15 room: loan 2,000 taken, insurance 107.15 taken (392.85 short).
        check(capped.voluntary(0).get(0).takenCents == 200_000 && capped.voluntary(0).get(1).takenCents == 10_715
            && capped.voluntary(0).get(1).shortfallCents == 39_285, "voluntary deductions are capped at what is left");
        check(capped.netCents(0) == 125_000, "net before allowances is 0; allowances 1,250 are still paid");
        check(capped.isBlocked(1) && capped.voluntaryCents(1) == 0, "a cutoff without pay takes no voluntary deductions");
        check(insurance.appliesTo(oct) && !insurance.appliesTo(oct.plusMonths(1)) && loan.appliesTo(oct.plusMonths(5)), "deduction month ranges");
        try {
            adjustments.addDeduction("DEMO-004", "Insurance", "", 100, oct, oct.minusMonths(1), "DEMO-002");
            throw new AssertionError("reversed months should be rejected");
        } catch (IllegalArgumentException expected) {
            check(true, "deduction month range must not be reversed");
        }
    }

    private static void overtimeRequestsFollowReviewRules() {
        EmployeeService employees = new EmployeeService(new InMemoryEmployeeStore());
        WorkflowService workflows = new WorkflowService(employees, new InMemoryWorkflowStore());
        String id = workflows.createOvertimeRequest("DEMO-002", "2026-10-01", 30, "close").getId();
        expect(IllegalArgumentException.class, () -> workflows.reviewOvertimeRequest(id, "approve", "DEMO-002"), "no self-approval of overtime");
        expect(IllegalStateException.class, () -> workflows.createOvertimeRequest("DEMO-002", "2026-10-01", 15, ""), "one open request per date");
        expect(IllegalArgumentException.class, () -> workflows.createOvertimeRequest("DEMO-002", "2026-10-02", 0, ""), "minutes must be positive");
        check("Rejected".equals(workflows.reviewOvertimeRequest(id, "reject", "DEMO-001").getStatus()), "another reviewer can reject");
        expect(IllegalStateException.class, () -> workflows.reviewOvertimeRequest(id, "approve", "DEMO-001"), "decided requests stay decided");
        check(workflows.createOvertimeRequest("DEMO-002", "2026-10-01", 15, "").getMinutes() == 15, "a rejected date can be requested again");
    }

    private static void add(AttendanceService service, String date, String in, String out) {
        service.createForEmployee("DEMO-001", java.time.LocalDate.parse(date), LocalTime.parse(in), LocalTime.parse(out));
    }

    private static void approve(WorkflowService workflows, String leaveId) {
        workflows.reviewLeaveRequest(leaveId, "approve", "DEMO-002");
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
        return new MonthPayroll("DEMO-001", oct, new java.math.BigDecimal("150.00"), java.util.Arrays.asList(
            new CutoffPay("1-15", oct.atDay(1), oct.atDay(15), 0, 0, new java.math.BigDecimal(firstGross)),
            new CutoffPay("16-end", oct.atDay(16), oct.atEndOfMonth(), 0, 0, new java.math.BigDecimal(secondGross))));
    }

    private static CutoffPayrollService service() {
        EmployeeService employees = new EmployeeService(new InMemoryEmployeeStore());
        return new CutoffPayrollService(new AttendanceService(employees, new InMemoryEmployeeScheduleStore(), new InMemoryAttendanceStore()));
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
