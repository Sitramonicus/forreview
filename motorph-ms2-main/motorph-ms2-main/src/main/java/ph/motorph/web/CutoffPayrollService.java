package ph.motorph.web;

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
    public MonthPayroll payrollForMonth(String employeeId, YearMonth month) {
        return payrollForMonth(employeeId, month, null);
    }

    public MonthPayroll payrollForMonth(String employeeId, YearMonth month, LocalDate through) {
        List<CutoffPay> cutoffs = grossForMonth(employeeId, month, through);
        BigDecimal rate = rateFor(employeeId, month).get();
        if (adjustments == null) {
            return new MonthPayroll(employeeId, month, rate, cutoffs, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), allowancesFor(employeeId), ratesFor(month));
        }
        return new MonthPayroll(employeeId, month, rate, cutoffs, adjustments.earningsFor(employeeId, month, 1),
            adjustments.earningsFor(employeeId, month, 2), adjustments.deductionsFor(employeeId, month),
            allowancesFor(employeeId), ratesFor(month));
    }

    private CutoffPay cutoff(String label, String employeeId, BigDecimal rate, LocalDate from, LocalDate periodEnd, LocalDate through,
            List<DemoLeaveRequest> leaves, Map<LocalDate, Integer> approvedOvertime) {
        // The cutoff keeps its real dates; only days after `through` are left out of the counting.
        LocalDate to = through != null && through.isBefore(periodEnd) ? through : periodEnd;
        BigDecimal basic = BigDecimal.ZERO, premium = BigDecimal.ZERO, overtime = BigDecimal.ZERO, night = BigDecimal.ZERO;
        long paid = 0, unpaid = 0, overtimeMinutes = 0, nightMinutes = 0;
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
            BigDecimal regular = BigDecimal.valueOf(m[PayrollProfile.REGULAR]);
            BigDecimal extra = BigDecimal.valueOf(m[PayrollProfile.OVERTIME]);
            basic = basic.add(regular);
            premium = premium.add(regular.multiply(kind.workedMultiplier.subtract(BigDecimal.ONE)));
            overtime = overtime.add(extra.multiply(kind.overtimeMultiplier));
            // Night differential stacks: 10% of the rate that applies to that minute (DOLE).
            night = night.add(BigDecimal.valueOf(m[PayrollProfile.REGULAR_NIGHT]).multiply(kind.workedMultiplier)
                .add(BigDecimal.valueOf(m[PayrollProfile.OVERTIME_NIGHT]).multiply(kind.overtimeMultiplier)).multiply(PayrollPolicy.nightDifferential()));
            paid += m[PayrollProfile.REGULAR];
            overtimeMinutes += m[PayrollProfile.OVERTIME];
            unpaid += m[PayrollProfile.UNPAID];
            nightMinutes += m[PayrollProfile.REGULAR_NIGHT] + m[PayrollProfile.OVERTIME_NIGHT];
            workedDates.add(record.getDate());
        }
        int holidayDays = 0, leaveDays = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (workedDates.contains(day) || !scheduleOn.apply(day).isWorkday(day) || !employedOn(employeeId, day)) continue;
            // shortcut: an unworked regular holiday is paid without checking attendance on the day before (Labor Code Art. 94 condition).
            if (HolidayCalendar2026.isRegularHoliday(day)) holidayDays++;
            else if (!HolidayCalendar2026.isHoliday(day) && onPaidLeave(leaves, employeeId, day)) leaveDays++; // workdays only (checked above)
        }
        return new CutoffPay(label, from, periodEnd, workedDates.size(), paid, unpaid, overtimeMinutes, nightMinutes, holidayDays, leaveDays,
            money(rate, basic), money(rate, premium), money(rate, overtime), money(rate, night),
            money(rate, BigDecimal.valueOf(PayrollPolicy.dailyMinutes()).multiply(BigDecimal.valueOf(holidayDays))),
            money(rate, BigDecimal.valueOf(PayrollPolicy.dailyMinutes()).multiply(BigDecimal.valueOf(leaveDays))));
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

    private static BigDecimal money(BigDecimal rate, BigDecimal weightedMinutes) {
        return rate.multiply(weightedMinutes).divide(SIXTY, 2, RoundingMode.HALF_UP);
    }
}
