package ph.motorph.web;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A person's working schedule, set by their supervisor: shift start and workdays. Pay follows it: the 10-minute grace
 * counts from the shift start, the unpaid meal hour starts 4 hours in, and non-workdays are rest days.
 * shortcut: no schedule history; a change applies to every date not yet finalized. Shifts start 05:00-15:00 so a
 * shift never crosses midnight.
 */
public final class WorkSchedule {
    public static final WorkSchedule DEFAULT = new WorkSchedule("", LocalTime.of(8, 0),
        EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), "", "");

    public final String employeeId, updatedBy, updatedAt;
    public final LocalTime shiftStart;
    public final Set<DayOfWeek> workdays;

    WorkSchedule(String employeeId, LocalTime shiftStart, Set<DayOfWeek> workdays, String updatedBy, String updatedAt) {
        this.employeeId = employeeId;
        this.shiftStart = shiftStart;
        this.workdays = Collections.unmodifiableSet(EnumSet.copyOf(workdays));
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public LocalTime graceEnd() { return shiftStart.plusMinutes(PayrollPolicy.graceMinutes()); }
    public LocalTime mealStart() { return shiftStart.plusHours(4); }
    public LocalTime mealEnd() { return shiftStart.plusHours(5); }
    /** The paid hours plus the meal hour (17:00 for an 08:00 start by default). */
    public LocalTime shiftEnd() { return shiftStart.plusMinutes(PayrollPolicy.dailyMinutes() + 60); }
    public boolean isWorkday(LocalDate date) { return workdays.contains(date.getDayOfWeek()); }

    /**
     * A single-day or multi-day change on top of the weekly pattern (design doc: "single-day, multi-day, and weekly
     * pattern shift assignments"): a different shift start on those dates, or a day off. The newest change wins.
     */
    public static final class DayChange {
        public final String id, employeeId, from, to, shiftStart, reason, setBy, setAt;

        DayChange(String id, String employeeId, String from, String to, String shiftStart, String reason, String setBy, String setAt) {
            this.id = id;
            this.employeeId = employeeId;
            this.from = from;
            this.to = to;
            this.shiftStart = shiftStart;
            this.reason = reason;
            this.setBy = setBy;
            this.setAt = setAt;
        }

        public boolean dayOff() { return shiftStart == null || shiftStart.isEmpty(); }

        boolean covers(LocalDate date) { return !date.isBefore(LocalDate.parse(from)) && !date.isAfter(LocalDate.parse(to)); }
    }

    /** In-memory store (local demo); people without an entry use DEFAULT (08:00, Monday-Friday). */
    public static final class Store {
        private final Map<String, WorkSchedule> schedules = new HashMap<>();
        private java.util.List<DayChange> dayChanges = new java.util.ArrayList<>();
        private int nextChange = 1;

        private java.util.List<DayChange> changes() {
            if (dayChanges == null) dayChanges = new java.util.ArrayList<>(); // older data files have none
            return dayChanges;
        }

        /** The schedule that applies on one date: the weekly pattern, or the newest day change covering that date. */
        public synchronized WorkSchedule forDate(String employeeId, LocalDate date) {
            WorkSchedule base = get(employeeId);
            DayChange change = null;
            for (DayChange item : changes()) if (item.employeeId.equals(employeeId) && item.covers(date)) change = item;
            if (change == null) return base;
            java.util.EnumSet<DayOfWeek> days = java.util.EnumSet.noneOf(DayOfWeek.class);
            days.addAll(base.workdays);
            if (change.dayOff()) days.remove(date.getDayOfWeek());
            else days.add(date.getDayOfWeek());
            if (days.isEmpty()) days.add(date.getDayOfWeek() == DayOfWeek.MONDAY ? DayOfWeek.TUESDAY : DayOfWeek.MONDAY); // keeps the type valid; this date stays off
            return new WorkSchedule(employeeId, change.dayOff() ? base.shiftStart : LocalTime.parse(change.shiftStart), days, change.setBy, change.setAt);
        }

        /** shiftStart null = a day off. One to 31 days, all in 2026. */
        public synchronized DayChange addDayChange(String employeeId, LocalDate from, LocalDate to, LocalTime shiftStart, String reason, String setBy) {
            if (to.isBefore(from)) throw new IllegalArgumentException("The last date must be on or after the first.");
            if (from.getYear() != 2026 || to.getYear() != 2026) throw new IllegalArgumentException("Dates must be in 2026 (the loaded payroll year).");
            if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 30) throw new IllegalArgumentException("A day change covers at most 31 days; use the weekly pattern for longer.");
            if (shiftStart != null && (shiftStart.isBefore(LocalTime.of(5, 0)) || shiftStart.isAfter(LocalTime.of(15, 0)))) {
                throw new IllegalArgumentException("Shift start must be between 05:00 and 15:00 (no overnight shifts yet).");
            }
            String text = reason == null ? "" : reason.trim();
            if (text.isEmpty() || text.length() > 200) throw new IllegalArgumentException("A reason (up to 200 characters) is required.");
            DayChange change = new DayChange(String.format("SC-%04d", nextChange++), employeeId, from.toString(), to.toString(),
                shiftStart == null ? "" : shiftStart.withSecond(0).withNano(0).toString(), text, setBy, Instant.now().toString());
            changes().add(change);
            return change;
        }

        public synchronized DayChange removeDayChange(String id) {
            for (java.util.Iterator<DayChange> it = changes().iterator(); it.hasNext(); ) {
                DayChange change = it.next();
                if (change.id.equals(id)) {
                    it.remove();
                    return change;
                }
            }
            throw new java.util.NoSuchElementException("Day change not found.");
        }

        public synchronized java.util.List<DayChange> dayChangesForAll() { return new java.util.ArrayList<>(changes()); }

        public synchronized java.util.List<DayChange> dayChangesFor(String employeeId) {
            java.util.List<DayChange> mine = new java.util.ArrayList<>();
            for (DayChange change : changes()) if (change.employeeId.equals(employeeId)) mine.add(change);
            return mine;
        }

        public synchronized WorkSchedule get(String employeeId) {
            WorkSchedule schedule = schedules.get(employeeId);
            return schedule == null ? new WorkSchedule(employeeId, DEFAULT.shiftStart, DEFAULT.workdays, "", "") : schedule;
        }

        public synchronized WorkSchedule set(String employeeId, LocalTime shiftStart, Set<DayOfWeek> workdays, String updatedBy) {
            if (shiftStart.isBefore(LocalTime.of(5, 0)) || shiftStart.isAfter(LocalTime.of(15, 0))) {
                throw new IllegalArgumentException("Shift start must be between 05:00 and 15:00 (no overnight shifts yet).");
            }
            if (workdays.isEmpty() || workdays.size() > 6) throw new IllegalArgumentException("Choose 1 to 6 workdays; everyone needs a rest day.");
            WorkSchedule schedule = new WorkSchedule(employeeId, shiftStart.withSecond(0).withNano(0), workdays, updatedBy, Instant.now().toString());
            schedules.put(employeeId, schedule);
            return schedule;
        }
    }
}
