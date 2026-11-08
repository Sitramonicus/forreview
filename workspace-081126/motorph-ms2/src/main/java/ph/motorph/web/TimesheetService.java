package ph.motorph.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns stored raw timecard rows into the two numbers a payslip needs: days worked and hours
 * paid. Only rows with a positive worked total count, so weekend and holiday rows recorded as
 * zero hours (as the supplied MotorPH timecard does) never inflate the day count.
 *
 * This reads; it never writes attendance and never classifies absence.
 */
public final class TimesheetService {

    public static final class Day {
        public final String date;
        public final String dayType;
        public final String hours;
        public Day(String date, String dayType, String hours) {
            this.date = date; this.dayType = dayType; this.hours = hours;
        }
    }

    public static final class Summary {
        private final String employeeId;
        private final String from;
        private final String to;
        private final int daysWorked;
        private final BigDecimal totalHours;
        private final List<Day> days;

        Summary(String employeeId, String from, String to, int daysWorked, BigDecimal totalHours, List<Day> days) {
            this.employeeId = employeeId; this.from = from; this.to = to;
            this.daysWorked = daysWorked; this.totalHours = totalHours; this.days = days;
        }

        public String employeeId() { return employeeId; }
        public String from() { return from; }
        public String to() { return to; }
        public int daysWorked() { return daysWorked; }
        public BigDecimal totalHours() { return totalHours; }
        public List<Day> days() { return days; }
    }

    private final AttendanceService attendanceService;

    public TimesheetService(AttendanceService attendanceService) {
        if (attendanceService == null) throw new IllegalArgumentException("attendance service must not be null");
        this.attendanceService = attendanceService;
    }

    public Summary summarise(String employeeId, LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("A timesheet period needs both a start and an end date.");
        List<AttendanceRecord> records = attendanceService.findForEmployee(employeeId, from, to);
        // One working day per calendar date, even if a date somehow carries more than one row.
        Map<String, Day> byDate = new LinkedHashMap<>();
        BigDecimal total = Money.ZERO;
        for (AttendanceRecord record : records) {
            BigDecimal hours = record.getHoursWorked();
            if (hours.signum() <= 0) continue;
            String key = record.getDate().toString();
            Day existing = byDate.get(key);
            if (existing == null) {
                byDate.put(key, new Day(key, record.getDayType(), hours.toPlainString()));
            } else {
                BigDecimal merged = new BigDecimal(existing.hours).add(hours);
                byDate.put(key, new Day(key, existing.dayType, merged.toPlainString()));
            }
            total = total.add(hours);
        }
        List<Day> days = new ArrayList<>(byDate.values());
        days.sort((a, b) -> a.date.compareTo(b.date));
        return new Summary(employeeId, from.toString(), to.toString(), days.size(), Money.round(total), days);
    }
}
