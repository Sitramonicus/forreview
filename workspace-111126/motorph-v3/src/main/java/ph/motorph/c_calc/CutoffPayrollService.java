package ph.motorph.c_calc;

import ph.motorph.o_oop.AttendanceRecord;
import ph.motorph.o_oop.HrRecords;
import ph.motorph.o_oop.LeavePolicy;
import ph.motorph.o_oop.PayrollProfile;
import ph.motorph.o_oop.WorkSchedule;
import ph.motorph.t_tkt.LeaveRequest;
import ph.motorph.t_tkt.OvertimeRequest;
import ph.motorph.t_tkt.WorkflowService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CutoffPayrollService {
    private static final BigDecimal SIXTY = new BigDecimal("60");
    private final AttendanceService attendanceService;
    private final WorkflowService workflowService;
    private PayrollAdjustments adjustments;
    private WorkSchedule.Store schedules;
    private SalaryHistory salaries;
    private HrRecords employment;
    private StatutoryRates.Registry rates;

    
    public CutoffPayrollService withRates(StatutoryRates.Registry rates) {
        this.rates = rates;
        return this;
    }

    private StatutoryRates ratesFor(YearMonth month) { return rates == null ? StatutoryRates.DEFAULT : rates.ratesFor(month); }

    
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

    
    public CutoffPayrollService withAdjustments(PayrollAdjustments adjustments) {
        this.adjustments = adjustments;
        return this;
    }

    
    public CutoffPayrollService(AttendanceService attendanceService, WorkflowService workflowService) {
        if (attendanceService == null) throw new IllegalArgumentException("attendance service must not be null");
        this.attendanceService = attendanceService;
        this.workflowService = workflowService;
    }

    
    public List<CutoffPay> grossForMonth(String employeeId, YearMonth month) {
        return grossForMonth(employeeId, month, null);
    }

    
    public List<CutoffPay> grossForMonth(String employeeId, YearMonth month, LocalDate through) {
        BigDecimal rate = rateFor(employeeId, month).orElseThrow(() ->
            new IllegalArgumentException("No monthly basic for " + employeeId
                + ". The employee file states one for every person on the roster, so this number is either not on the"
                + " roster or has no salary recorded."));
        List<LeaveRequest> leaves = workflowService == null ? Collections.emptyList() : workflowService.listLeaveRequests();
        Map<LocalDate, Integer> overtime = new HashMap<>();
        if (workflowService != null) {
            for (OvertimeRequest request : workflowService.listOvertimeRequests()) {
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

    
    
    private MonthPayroll.Split split = MonthPayroll.Split.FULL_FIRST;

    public CutoffPayrollService withSplit(MonthPayroll.Split chosen) {
        this.split = chosen == null ? MonthPayroll.Split.FULL_FIRST : chosen;
        return this;
    }

    
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
            List<LeaveRequest> leaves, Map<LocalDate, Integer> approvedOvertime) {
        
        
        BigDecimal dailyRate = PayrollProfile.dailyRate(employeeId).orElse(
            rate.multiply(BigDecimal.valueOf(8)));
        
        LocalDate to = through != null && through.isBefore(periodEnd) ? through : periodEnd;
        BigDecimal premium = BigDecimal.ZERO, overtime = BigDecimal.ZERO, night = BigDecimal.ZERO;
        long paid = 0, unpaid = 0, overtimeMinutes = 0, nightMinutes = 0;
        
        
        
        long wholeWorkedDays = 0;
        BigDecimal fractionWorkedDays = BigDecimal.ZERO;
        Set<LocalDate> workedDates = new HashSet<>();
        
        java.util.function.Function<LocalDate, WorkSchedule> scheduleOn = date -> schedules == null ? WorkSchedule.DEFAULT : schedules.forDate(employeeId, date);
        List<AttendanceRecord> records = to.isBefore(from) ? Collections.emptyList() : attendanceService.findForEmployee(employeeId, from, to);
        for (AttendanceRecord record : records) {
            if (!employedOn(employeeId, record.getDate())) continue; 
            WorkSchedule schedule = scheduleOn.apply(record.getDate());
            HolidayCalendar2026.DayKind kind = HolidayCalendar2026.kindOf(record.getDate(), !schedule.isWorkday(record.getDate()));
            long[] m = PayrollProfile.splitDay(record.getTimeIn(), record.getTimeOut(),
                approvedOvertime.getOrDefault(record.getDate(), 0), schedule);
            
            long scheduled = PayrollPolicy.dailyMinutes();
            long workedRegular = m[PayrollProfile.REGULAR];
            boolean fullDay = workedRegular >= scheduled;
            if (fullDay) wholeWorkedDays++;
            else fractionWorkedDays = fractionWorkedDays.add(
                BigDecimal.valueOf(workedRegular).divide(BigDecimal.valueOf(scheduled), 10, java.math.RoundingMode.HALF_UP));
            BigDecimal extra = BigDecimal.valueOf(m[PayrollProfile.OVERTIME]);
            
            
            
            
            
            
            BigDecimal workedDayUnits = BigDecimal.valueOf(Math.min(workedRegular, scheduled))
                .divide(BigDecimal.valueOf(scheduled), 10, java.math.RoundingMode.HALF_UP);
            premium = premium.add(workedDayUnits.multiply(kind.workedMultiplier.subtract(BigDecimal.ONE)));
            overtime = overtime.add(extra.divide(SIXTY, 6, java.math.RoundingMode.HALF_UP).multiply(kind.overtimeMultiplier));
            
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

    
    private boolean presentOnPrecedingWorkday(String employeeId, LocalDate holiday,
            java.util.function.Function<LocalDate, WorkSchedule> scheduleOn, Set<LocalDate> workedDates) {
        for (LocalDate day = holiday.minusDays(1), first = holiday.minusDays(14); !day.isBefore(first); day = day.minusDays(1)) {
            if (!scheduleOn.apply(day).isWorkday(day) || HolidayCalendar2026.isHoliday(day)) continue;
            
            return workedDates.contains(day);
        }
        return false; 
    }

    private static boolean onPaidLeave(List<LeaveRequest> leaves, String employeeId, LocalDate day) {
        for (LeaveRequest leave : leaves) {
            if (leave.getEmployeeId().equals(employeeId) && "Approved".equals(leave.getStatus())
                    && LeavePolicy.isPaid(leave.getLeaveType())
                    && !day.isBefore(LocalDate.parse(leave.getStartDate())) && !day.isAfter(LocalDate.parse(leave.getEndDate()))) {
                return true;
            }
        }
        return false;
    }
    
    private static BigDecimal dayPay(BigDecimal dailyRate, long wholeDays, BigDecimal fractionDays) {
        return dailyRate.multiply(BigDecimal.valueOf(wholeDays)).add(dailyRate.multiply(fractionDays))
            .setScale(2, RoundingMode.HALF_UP);
    }

    
    private static BigDecimal paidDays(BigDecimal dailyRate, BigDecimal dayUnits) {
        return dailyRate.multiply(dayUnits).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal hourlyRate, BigDecimal hourUnits) {
        return hourlyRate.multiply(hourUnits).setScale(2, RoundingMode.HALF_UP);
    }
}
