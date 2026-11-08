package ph.motorph.c_calc;

import ph.motorph.o_oop.AttendanceRecord;
import ph.motorph.o_oop.HrRecords;
import ph.motorph.o_oop.LeavePolicy;
import ph.motorph.o_oop.PayrollProfile;
import ph.motorph.o_oop.WorkSchedule;
import ph.motorph.t_tkt.DemoLeaveRequest;
import ph.motorph.t_tkt.DemoOvertimeRequest;
import ph.motorph.t_tkt.WorkflowService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns raw attendance, approved paid leave and approved overtime into earnings per cutoff (PayrollProfile rules,
 * HolidayCalendar2026 multipliers). Each earnings line is rate x weighted minutes / 60, rounded half-up once.
 */
public final class CutoffPayrollService {
    private static final BigDecimal SIXTY = new BigDecimal("60");
    private final AttendanceService attendanceService;
    private final WorkflowService workflowService;
    private PayrollAdjustments adjustments;
    private WorkSchedule.Store schedules;
    private SalaryHistory salaries;
    private HrRecords employment;
    private StatutoryRates.Registry rates;

    /** Admin rates versions by effective month; null = the built-in 2026 rates. */
    public CutoffPayrollService withRates(StatutoryRates.Registry rates) {
        this.rates = rates;
        return this;
    }

    private StatutoryRates ratesFor(YearMonth month) { return rates == null ? StatutoryRates.DEFAULT : rates.ratesFor(month); }

    /** Start dates, last days and new hires' allowances; null = seeded people only. */
    public CutoffPayrollService withEmployment(HrRecords employment) {
        this.employment = employment;
        return this;
    }

    private boolean employedOn(String employeeId, LocalDate day) {
        return employment == null || employment.find(employeeId).map(record -> record.employedOn(day)).orElse(true);
    }

    private long[] allowancesFor(String employeeId) {
        return employment == null ? PayrollProfile.monthlyAllowances(employeeId) : employment.monthlyAllowances(employeeId);
    }

    /** Work schedules (shift start, rest days) and approved salary changes; null = defaults. */
    public CutoffPayrollService withSchedules(WorkSchedule.Store schedules, SalaryHistory salaries) {
        this.schedules = schedules;
        this.salaries = salaries;
        return this;
    }

    private java.util.Optional<BigDecimal> rateFor(String employeeId, YearMonth month) {
        return salaries == null ? PayrollProfile.hourlyRate(employeeId) : salaries.hourlyRate(employeeId, month);
    }

    public CutoffPayrollService(AttendanceService attendanceService) {
        this(attendanceService, null);
    }

    /** Adds Payroll Staff bonuses and voluntary deductions to payrollForMonth; null = none. */
    public CutoffPayrollService withAdjustments(PayrollAdjustments adjustments) {
        this.adjustments = adjustments;
        return this;
    }

    /** workflowService may be null: then no leave pay and no approved overtime. */
    public CutoffPayrollService(AttendanceService attendanceService, WorkflowService workflowService) {
        if (attendanceService == null) throw new IllegalArgumentException("attendance service must not be null");
        this.attendanceService = attendanceService;
        this.workflowService = workflowService;
    }

    /** Returns the 1-15 cutoff then the 16-month-end cutoff. Throws if the employee has no monthly basic. */
    public List<CutoffPay> grossForMonth(String employeeId, YearMonth month) {
        return grossForMonth(employeeId, month, null);
    }

    /** Same, counting only days up to and including {@code through} (null = whole month); used by the salary tracker. */
    public List<CutoffPay> grossForMonth(String employeeId, YearMonth month, LocalDate through) {
        BigDecimal rate = rateFor(employeeId, month).orElseThrow(() ->
            new IllegalArgumentException("No temporary monthly basic for " + employeeId + "; only DEMO-001 to DEMO-005 have one."));
        List<DemoLeaveRequest> leaves = workflowService == null ? Collections.emptyList() : workflowService.listLeaveRequests();
        Map<LocalDate, Integer> overtime = new HashMap<>();
        if (workflowService != null) {
            for (DemoOvertimeRequest request : workflowService.listOvertimeRequests()) {
                if (request.getEmployeeId().equals(employeeId) && "Approved".equals(request.getStatus())) {
                    overtime.put(LocalDate.parse(request.getDate()), request.getMinutes());
                }
            }
        }
        List<CutoffPay> cutoffs = new ArrayList<>();
        cutoffs.add(cutoff("1-15", employeeId, rate, month.atDay(1), month.atDay(15), through, leaves, overtime));
        cutoffs.add(cutoff("16-end", employeeId, rate, month.atDay(16), month.atEndOfMonth(), through, leaves, overtime));
        return cutoffs;
    }

    /** Earnings per cutoff plus statutory deductions split half per cutoff (see MonthPayroll). */
    /** Statutory split between the two cut-offs. MotorPH's own sample slip charges the month in the 1–15 cut-off. */
    private MonthPayroll.Split split = MonthPayroll.Split.FULL_FIRST;

    public CutoffPayrollService withSplit(MonthPayroll.Split chosen) {
        this.split = chosen == null ? MonthPayroll.Split.FULL_FIRST : chosen;
        return this;
    }

    /** The contribution base: the monthly basic the employee file states, in centavos. */
    private long monthlyBasicCents(String employeeId, YearMonth month) {
        java.util.Optional<BigDecimal> basic = salaries == null
            ? PayrollProfile.monthlyBasic(employeeId)
            : salaries.monthlyBasic(employeeId, month);
        return basic.map(amount -> amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()).orElse(0L);
    }

    public MonthPayroll payrollForMonth(String employeeId, YearMonth month) {
        return payrollForMonth(employeeId, month, null);
    }

    public MonthPayroll payrollForMonth(String employeeId, YearMonth month, LocalDate through) {
        List<CutoffPay> cutoffs = grossForMonth(employeeId, month, through);
        BigDecimal rate = rateFor(employeeId, month).get();
        long basicCents = monthlyBasicCents(employeeId, month);
        if (adjustments == null) {
            return new MonthPayroll(employeeId, month, rate, cutoffs, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), allowancesFor(employeeId), ratesFor(month), split, basicCents);
        }
        return new MonthPayroll(employeeId, month, rate, cutoffs, adjustments.earningsFor(employeeId, month, 1),
            adjustments.earningsFor(employeeId, month, 2), adjustments.deductionsFor(employeeId, month),
            allowancesFor(employeeId), ratesFor(month), split, basicCents);
    }

    private CutoffPay cutoff(String label, String employeeId, BigDecimal rate, LocalDate from, LocalDate periodEnd, LocalDate through,
            List<DemoLeaveRequest> leaves, Map<LocalDate, Integer> approvedOvertime) {
        // Daily rate = monthly basic / 22 working days, which is the divisor MotorPH's own payslip uses
        // (53,500 / 22 = 2,431.82, and the file's semi-monthly rate of 26,750 / 11 days = 2,431.82 too).
        BigDecimal dailyRate = PayrollProfile.dailyRate(employeeId).orElse(
            rate.multiply(BigDecimal.valueOf(8)));
        // The cutoff keeps its real dates; only days after `through` are left out of the counting.
        LocalDate to = through != null && through.isBefore(periodEnd) ? through : periodEnd;
        BigDecimal basic = BigDecimal.ZERO, premium = BigDecimal.ZERO, overtime = BigDecimal.ZERO, night = BigDecimal.ZERO;
        long paid = 0, unpaid = 0, overtimeMinutes = 0, nightMinutes = 0;
        // A monthly-salaried person is paid per day worked, not per minute worked: 53,500 a month is 2,675 a day,
        // and ten days is 26,750 — which is exactly what MotorPH's sample payslip shows. Paying by the minute
        // (rate/60 x minutes) loses the minutes either side of the shift and understates every payslip.
        long wholeWorkedDays = 0;
        BigDecimal fractionWorkedDays = BigDecimal.ZERO;
        Set<LocalDate> workedDates = new HashSet<>();
        // Each date uses its own schedule: the weekly pattern or a single-day/multi-day change.
        java.util.function.Function<LocalDate, WorkSchedule> scheduleOn = date -> schedules == null ? WorkSchedule.DEFAULT : schedules.forDate(employeeId, date);
        List<AttendanceRecord> records = to.isBefore(from) ? Collections.emptyList() : attendanceService.findForEmployee(employeeId, from, to);
        for (AttendanceRecord record : records) {
            if (!employedOn(employeeId, record.getDate())) continue; // nothing before the start date or after the last day
            WorkSchedule schedule = scheduleOn.apply(record.getDate());
            HolidayCalendar2026.DayKind kind = HolidayCalendar2026.kindOf(record.getDate(), !schedule.isWorkday(record.getDate()));
            long[] m = PayrollProfile.splitDay(record.getTimeIn(), record.getTimeOut(),
                approvedOvertime.getOrDefault(record.getDate(), 0), schedule);
            // Whole-shift attendance pays a day; a part-day pays the fraction of the scheduled day actually worked.
            long scheduled = PayrollPolicy.dailyMinutes();
            long workedRegular = m[PayrollProfile.REGULAR];
            boolean fullDay = workedRegular >= scheduled;
            if (fullDay) wholeWorkedDays++;
            else fractionWorkedDays = fractionWorkedDays.add(
                BigDecimal.valueOf(workedRegular).divide(BigDecimal.valueOf(scheduled), 10, java.math.RoundingMode.HALF_UP));
            BigDecimal extra = BigDecimal.valueOf(m[PayrollProfile.OVERTIME]);
            // Only the part of a day beyond a full shift earns the day-type premium; a normal full day earns none.
            BigDecimal extraDayUnits = fullDay
                ? BigDecimal.valueOf(Math.max(0, workedRegular - scheduled)).divide(BigDecimal.valueOf(scheduled), 10, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            premium = premium.add(extraDayUnits.multiply(kind.workedMultiplier.subtract(BigDecimal.ONE)));
            overtime = overtime.add(extra.divide(SIXTY, 6, java.math.RoundingMode.HALF_UP).multiply(kind.overtimeMultiplier));
            // Night differential stacks: 10% of the rate that applies to that minute (DOLE).
            night = night.add(BigDecimal.valueOf(m[PayrollProfile.REGULAR_NIGHT]).multiply(kind.workedMultiplier)
                .add(BigDecimal.valueOf(m[PayrollProfile.OVERTIME_NIGHT]).multiply(kind.overtimeMultiplier))
                .divide(SIXTY, 6, java.math.RoundingMode.HALF_UP).multiply(PayrollPolicy.nightDifferential()));
            paid += m[PayrollProfile.REGULAR];
            overtimeMinutes += m[PayrollProfile.OVERTIME];
            unpaid += m[PayrollProfile.UNPAID];
            nightMinutes += m[PayrollProfile.REGULAR_NIGHT] + m[PayrollProfile.OVERTIME_NIGHT];
            workedDates.add(record.getDate());
        }
        int holidayDays = 0, leaveDays = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (workedDates.contains(day) || !employedOn(employeeId, day)) continue;
            // A regular holiday is paid whether or not it is worked. If it was worked, the day already counted as
            // basic pay above, so only an *unworked* regular holiday adds a day here — otherwise it is paid twice.
            // Holidays count on any calendar day; leave pay needs an ordinary scheduled workday.
            if (HolidayCalendar2026.isRegularHoliday(day) && presentOnPrecedingWorkday(employeeId, day, scheduleOn, workedDates)) {
                holidayDays++;
            } else if (scheduleOn.apply(day).isWorkday(day) && !HolidayCalendar2026.isHoliday(day)
                    && onPaidLeave(leaves, employeeId, day)) {
                leaveDays++;
            }
        }
        return new CutoffPay(label, from, periodEnd, workedDates.size(), paid, unpaid, overtimeMinutes, nightMinutes, holidayDays, leaveDays,
            dayPay(dailyRate, wholeWorkedDays, fractionWorkedDays), paidDays(dailyRate, premium), money(rate, overtime), money(rate, night),
            paidDays(dailyRate, BigDecimal.valueOf(holidayDays)), paidDays(dailyRate, BigDecimal.valueOf(leaveDays)));
    }

    /**
     * Labor Code Art. 94: an employee who does not work on a regular holiday is still paid that day <em>only</em> if
     * the employee was present, or on paid leave, on the workday immediately preceding the holiday. Without this check
     * a 1–15 cut-off starting on January 1st would hand out a free holiday day: the sample slip pays ten days,
     * 26,750.00, and nothing for the New Year.
     */
    private boolean presentOnPrecedingWorkday(String employeeId, LocalDate holiday,
            java.util.function.Function<LocalDate, WorkSchedule> scheduleOn, Set<LocalDate> workedDates) {
        for (LocalDate day = holiday.minusDays(1), first = holiday.minusDays(14); !day.isBefore(first); day = day.minusDays(1)) {
            if (!scheduleOn.apply(day).isWorkday(day) || HolidayCalendar2026.isHoliday(day)) continue;
            // The preceding workday decides it: present (a record exists) or on paid leave.
            return workedDates.contains(day);
        }
        return false; // no preceding workday on record means no qualifying attendance
    }

    private static boolean onPaidLeave(List<DemoLeaveRequest> leaves, String employeeId, LocalDate day) {
        for (DemoLeaveRequest leave : leaves) {
            if (leave.getEmployeeId().equals(employeeId) && "Approved".equals(leave.getStatus())
                    && LeavePolicy.isPaid(leave.getLeaveType())
                    && !day.isBefore(LocalDate.parse(leave.getStartDate())) && !day.isAfter(LocalDate.parse(leave.getEndDate()))) {
                return true;
            }
        }
        return false;
    }

    /** Hours are paid at the hourly rate: an overtime premium of half an hour is rate x 0.5. */
    private static BigDecimal money(BigDecimal rate, BigDecimal weightedHours) {
        return rate.multiply(weightedHours).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * A worked day expressed as whole days plus a fraction. Whole days are carried exactly so that ten full days at
     * 2,431.82 gives 24,318.20 and never 24,318.19, which a single pre-rounded day-unit value would.
     */
    private static long[] wholeDays(long workedMinutes, long scheduledMinutes) {
        if (workedMinutes >= scheduledMinutes) return new long[]{ 1, 0, scheduledMinutes };
        return new long[]{ 0, workedMinutes, scheduledMinutes };
    }

    /** The fractional part of a day, as a decimal, or zero for a full day. */
    private static BigDecimal partOf(long[] dayUnits) {
        return dayUnits[1] == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(dayUnits[1]).divide(BigDecimal.valueOf(dayUnits[2]), 10, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Whole days at the stated daily rate plus any part-days, rounded once at the end. Ten days at 2,431.82 gives
     * 24,318.20 exactly; rounding the daily rate per day first is what produced a two-centavo drift.
     */
    private static BigDecimal dayPay(BigDecimal dailyRate, long wholeDays, BigDecimal fractionDays) {
        return dailyRate.multiply(BigDecimal.valueOf(wholeDays)).add(dailyRate.multiply(fractionDays))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /** Days are paid at the daily rate, which the employee file states directly. */
    private static BigDecimal paidDays(BigDecimal dailyRate, BigDecimal dayUnits) {
        return dailyRate.multiply(dayUnits).setScale(2, RoundingMode.HALF_UP);
    }
}
