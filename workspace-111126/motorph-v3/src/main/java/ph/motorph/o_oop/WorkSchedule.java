package ph.motorph.o_oop;

import com.google.gson.annotations.SerializedName;
import ph.motorph.c_calc.PayrollPolicy;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class WorkSchedule {
    public static final WorkSchedule DEFAULT = new WorkSchedule("", LocalTime.of(8, 0),
        EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), "", "");

    @SerializedName("employeeId") public final String ws_sEid;
    @SerializedName("updatedBy") public final String ws_sUpdatedBy;
    @SerializedName("updatedAt") public final String ws_sUpdatedAt;
    @SerializedName("shiftStart") public final LocalTime ws_tShiftStart;
    @SerializedName("workdays") public final Set<DayOfWeek> ws_setWorkdays;

    WorkSchedule(String employeeId, LocalTime shiftStart, Set<DayOfWeek> workdays, String updatedBy, String updatedAt) {
        this.ws_sEid = employeeId;
        this.ws_tShiftStart = shiftStart;
        this.ws_setWorkdays = Collections.unmodifiableSet(EnumSet.copyOf(workdays));
        this.ws_sUpdatedBy = updatedBy;
        this.ws_sUpdatedAt = updatedAt;
    }

    public LocalTime graceEnd() { return ws_tShiftStart.plusMinutes(PayrollPolicy.graceMinutes()); }
    public LocalTime mealStart() { return ws_tShiftStart.plusHours(4); }
    public LocalTime mealEnd() { return ws_tShiftStart.plusHours(5); }
    
    public LocalTime shiftEnd() { return ws_tShiftStart.plusMinutes(PayrollPolicy.dailyMinutes() + 60); }
    public boolean isWorkday(LocalDate date) { return ws_setWorkdays.contains(date.getDayOfWeek()); }

    
    public static final class DayChange {
        @SerializedName("id") public final String dc_sId;
        @SerializedName("employeeId") public final String dc_sEid;
        @SerializedName("from") public final String dc_dFrom;
        @SerializedName("to") public final String dc_dTo;
        @SerializedName("shiftStart") public final String dc_tShiftStart;
        @SerializedName("reason") public final String dc_sReason;
        @SerializedName("setBy") public final String dc_sSetBy;
        @SerializedName("setAt") public final String dc_sSetAt;

        DayChange(String id, String employeeId, String from, String to, String shiftStart, String reason, String setBy, String setAt) {
            this.dc_sId = id;
            this.dc_sEid = employeeId;
            this.dc_dFrom = from;
            this.dc_dTo = to;
            this.dc_tShiftStart = shiftStart;
            this.dc_sReason = reason;
            this.dc_sSetBy = setBy;
            this.dc_sSetAt = setAt;
        }

        public boolean dayOff() { return dc_tShiftStart == null || dc_tShiftStart.isEmpty(); }

        boolean covers(LocalDate date) { return !date.isBefore(LocalDate.parse(dc_dFrom)) && !date.isAfter(LocalDate.parse(dc_dTo)); }
    }

    
    public static final class Store {
        @SerializedName("schedules") private final Map<String, WorkSchedule> ws_mSchedules = new HashMap<>();
        @SerializedName("dayChanges") private java.util.List<DayChange> ws_lDayChanges = new java.util.ArrayList<>();
        @SerializedName("nextChange") private int ws_iNextChange = 1;

        private java.util.List<DayChange> changes() {
            if (ws_lDayChanges == null) ws_lDayChanges = new java.util.ArrayList<>(); 
            return ws_lDayChanges;
        }

        
        public synchronized WorkSchedule forDate(String employeeId, LocalDate date) {
            WorkSchedule base = get(employeeId);
            DayChange change = null;
            for (DayChange item : changes()) if (item.dc_sEid.equals(employeeId) && item.covers(date)) change = item;
            if (change == null) return base;
            java.util.EnumSet<DayOfWeek> days = java.util.EnumSet.noneOf(DayOfWeek.class);
            days.addAll(base.ws_setWorkdays);
            if (change.dayOff()) days.remove(date.getDayOfWeek());
            else days.add(date.getDayOfWeek());
            if (days.isEmpty()) days.add(date.getDayOfWeek() == DayOfWeek.MONDAY ? DayOfWeek.TUESDAY : DayOfWeek.MONDAY); 
            return new WorkSchedule(employeeId, change.dayOff() ? base.ws_tShiftStart : LocalTime.parse(change.dc_tShiftStart), days, change.dc_sSetBy, change.dc_sSetAt);
        }

        
        public synchronized DayChange addDayChange(String employeeId, LocalDate from, LocalDate to, LocalTime shiftStart, String reason, String setBy) {
            if (to.isBefore(from)) throw new IllegalArgumentException("The last date must be on or after the first.");
            if (from.getYear() != 2026 || to.getYear() != 2026) throw new IllegalArgumentException("Dates must be in 2026 (the loaded payroll year).");
            if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 30) throw new IllegalArgumentException("A day change covers at most 31 days; use the weekly pattern for longer.");
            if (shiftStart != null && (shiftStart.isBefore(LocalTime.of(5, 0)) || shiftStart.isAfter(LocalTime.of(15, 0)))) {
                throw new IllegalArgumentException("Shift start must be between 05:00 and 15:00 (no overnight shifts yet).");
            }
            String text = reason == null ? "" : reason.trim();
            if (text.isEmpty() || text.length() > 200) throw new IllegalArgumentException("A reason (up to 200 characters) is required.");
            DayChange change = new DayChange(String.format("SC-%04d", ws_iNextChange++), employeeId, from.toString(), to.toString(),
                shiftStart == null ? "" : shiftStart.withSecond(0).withNano(0).toString(), text, setBy, Instant.now().toString());
            changes().add(change);
            return change;
        }

        public synchronized DayChange removeDayChange(String id) {
            for (java.util.Iterator<DayChange> it = changes().iterator(); it.hasNext(); ) {
                DayChange change = it.next();
                if (change.dc_sId.equals(id)) {
                    it.remove();
                    return change;
                }
            }
            throw new java.util.NoSuchElementException("Day change not found.");
        }

        public synchronized java.util.List<DayChange> dayChangesForAll() { return new java.util.ArrayList<>(changes()); }

        public synchronized java.util.List<DayChange> dayChangesFor(String employeeId) {
            java.util.List<DayChange> mine = new java.util.ArrayList<>();
            for (DayChange change : changes()) if (change.dc_sEid.equals(employeeId)) mine.add(change);
            return mine;
        }

        public synchronized WorkSchedule get(String employeeId) {
            WorkSchedule schedule = ws_mSchedules.get(employeeId);
            return schedule == null ? new WorkSchedule(employeeId, DEFAULT.ws_tShiftStart, DEFAULT.ws_setWorkdays, "", "") : schedule;
        }

        public synchronized WorkSchedule set(String employeeId, LocalTime shiftStart, Set<DayOfWeek> workdays, String updatedBy) {
            if (shiftStart.isBefore(LocalTime.of(5, 0)) || shiftStart.isAfter(LocalTime.of(15, 0))) {
                throw new IllegalArgumentException("Shift start must be between 05:00 and 15:00 (no overnight shifts yet).");
            }
            if (workdays.isEmpty() || workdays.size() > 6) throw new IllegalArgumentException("Choose 1 to 6 workdays; everyone needs a rest day.");
            WorkSchedule schedule = new WorkSchedule(employeeId, shiftStart.withSecond(0).withNano(0), workdays, updatedBy, Instant.now().toString());
            ws_mSchedules.put(employeeId, schedule);
            return schedule;
        }
    }
}
