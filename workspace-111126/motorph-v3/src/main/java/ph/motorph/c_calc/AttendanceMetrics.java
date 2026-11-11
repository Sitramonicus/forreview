package ph.motorph.c_calc;

import ph.motorph.o_oop.AttendanceRecord;
import ph.motorph.o_oop.PayrollProfile;
import ph.motorph.o_oop.WorkSchedule;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class AttendanceMetrics {
    public final LocalDate from, to;
    public final int presentDays, absentDays, tardyDays, leaveDays, holidayDays;
    public final long tardyMinutes, overtimeMinutes, unapprovedExtraMinutes, undertimeMinutes;

    private AttendanceMetrics(LocalDate from, LocalDate to, int presentDays, int absentDays, int tardyDays, int leaveDays, int holidayDays,
            long tardyMinutes, long overtimeMinutes, long unapprovedExtraMinutes, long undertimeMinutes) {
        this.from = from;
        this.to = to;
        this.presentDays = presentDays;
        this.absentDays = absentDays;
        this.tardyDays = tardyDays;
        this.leaveDays = leaveDays;
        this.holidayDays = holidayDays;
        this.tardyMinutes = tardyMinutes;
        this.overtimeMinutes = overtimeMinutes;
        this.unapprovedExtraMinutes = unapprovedExtraMinutes;
        this.undertimeMinutes = undertimeMinutes;
    }

    
    public static AttendanceMetrics of(List<AttendanceRecord> records, LocalDate from, LocalDate to, LocalDate today, WorkSchedule schedule,
            Map<LocalDate, Integer> approvedOvertime, Predicate<LocalDate> onLeave, Predicate<LocalDate> employed) {
        return of(records, from, to, today, date -> schedule, approvedOvertime, onLeave, employed);
    }

    
    public static AttendanceMetrics of(List<AttendanceRecord> records, LocalDate from, LocalDate to, LocalDate today,
            java.util.function.Function<LocalDate, WorkSchedule> scheduleOn,
            Map<LocalDate, Integer> approvedOvertime, Predicate<LocalDate> onLeave, Predicate<LocalDate> employed) {
        LocalDate start = from, end = to;
        for (AttendanceRecord record : records) {
            if (from == null && (start == null || record.getDate().isBefore(start))) start = record.getDate();
            if (to == null && (end == null || record.getDate().isAfter(end))) end = record.getDate();
        }
        int present = 0, tardy = 0, absent = 0, leave = 0, holidays = 0;
        long tardyMinutes = 0, overtime = 0, extra = 0, undertime = 0;
        Set<LocalDate> worked = new HashSet<>();
        for (AttendanceRecord record : records) {
            worked.add(record.getDate());
            present++;
            WorkSchedule schedule = scheduleOn.apply(record.getDate());
            long late = java.time.Duration.between(schedule.ws_tShiftStart, record.getTimeIn()).toMinutes();
            if (record.getTimeIn().isAfter(schedule.graceEnd())) {
                tardy++;
                tardyMinutes += late;
            }
            long[] m = PayrollProfile.splitDay(record.getTimeIn(), record.getTimeOut(), approvedOvertime.getOrDefault(record.getDate(), 0), schedule);
            overtime += m[PayrollProfile.OVERTIME];
            extra += m[PayrollProfile.UNPAID];
            
            if (schedule.isWorkday(record.getDate())) undertime += Math.max(0, PayrollPolicy.dailyMinutes() - m[PayrollProfile.REGULAR]);
        }
        if (start != null && end != null) {
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                if (worked.contains(day) || !scheduleOn.apply(day).isWorkday(day) || !employed.test(day)) continue;
                if (HolidayCalendar2026.isHoliday(day)) holidays++;
                else if (onLeave.test(day)) leave++;
                else if (!day.isAfter(today)) absent++;
            }
        }
        return new AttendanceMetrics(start, end, present, absent, tardy, leave, holidays, tardyMinutes, overtime, extra, undertime);
    }
}
