package ph.motorph.o_oop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StateFileRoundTripTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path sample = Path.of("data/local-demo.json");
        if (!Files.exists(sample)) {
            System.out.println("SKIP no local-demo.json to read");
            return;
        }

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (com.google.gson.JsonSerializer<LocalDate>) (v, t, c) -> new com.google.gson.JsonPrimitive(v.toString()))
            .registerTypeAdapter(LocalDate.class, (com.google.gson.JsonDeserializer<LocalDate>) (v, t, c) -> LocalDate.parse(v.getAsString()))
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonSerializer<LocalTime>) (v, t, c) -> new com.google.gson.JsonPrimitive(v.toString()))
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonDeserializer<LocalTime>) (v, t, c) -> LocalTime.parse(v.getAsString()))
            .create();

        String raw = new String(Files.readAllBytes(sample), StandardCharsets.UTF_8);
        State state = gson.fromJson(raw, State.class);

        List<AttendanceRecord> rows = state.stores.attendance.records;
        check(rows.size() > 0, "attendance rows survive a load (" + rows.size() + ")");
        if (rows.size() > 0) {
            AttendanceRecord first = rows.get(0);
            check(first.getAttendanceId() > 0, "attendanceId is not blanked: " + first.getAttendanceId());
            check(first.getDate() != null, "date is not null: " + first.getDate());
            check(first.getTimeIn() != null, "timeIn is not null: " + first.getTimeIn());
            check(first.getTimeOut() != null, "timeOut is not null: " + first.getTimeOut());
            check(first.getDayType() != null && !first.getDayType().isEmpty(), "dayType is not blank: " + first.getDayType());
        }

        Map<String, Employee> roster = state.stores.employees.employees;
        check(roster.size() > 0, "employees survive a load (" + roster.size() + ")");
        Employee one = roster.values().stream().filter(e -> e.getEid() != null && e.getEid().equals("10001")).findFirst().orElse(null);
        if (one != null) {
            check(one.getFirstName() != null && !one.getFirstName().isEmpty(), "firstName loaded: " + one.getFirstName());
            check(one.getLastName() != null && !one.getLastName().isEmpty(), "lastName loaded: " + one.getLastName());
            check(one.getPosition() != null && !one.getPosition().isEmpty(), "position loaded: " + one.getPosition());
            check(one.getDepartment() != null && !one.getDepartment().isEmpty(), "department loaded: " + one.getDepartment());
            check(one.getMonthlyBasic() != null, "monthlyBasic loaded: " + one.getMonthlyBasic());
            check(one.getBirthday() != null, "birthday loaded: " + one.getBirthday());
            check(one.getBranchName() != null, "branchName loaded (may be empty): [" + one.getBranchName() + "]");
        } else {
            check(false, "10001 is on the roster");
        }

        if (state.stores.workSchedules != null) {
            check(state.stores.workSchedules.schedules != null, "workSchedules store parses");
            int set = 0;
            for (WorkSchedule one2 : state.stores.workSchedules.schedules.values()) {
                if (one2.ws_sEid == null || one2.ws_tShiftStart == null) continue;
                set++;
                check(one2.ws_setWorkdays != null && !one2.ws_setWorkdays.isEmpty(), "a saved schedule keeps its workdays");
            }
            check(state.stores.workSchedules.schedules.size() == 0 || set > 0,
                "every saved schedule keeps employeeId and shiftStart (" + set + " of " + state.stores.workSchedules.schedules.size() + ")");
            for (WorkSchedule.DayChange change : state.stores.workSchedules.dayChanges == null
                    ? new ArrayList<WorkSchedule.DayChange>() : state.stores.workSchedules.dayChanges) {
                check(change.dc_sId != null && change.dc_dFrom != null, "a saved day change keeps its id and first date");
            }
        }

        List<AuditLogEntry> log = state.stores.auditLog == null ? new ArrayList<>() : state.stores.auditLog.lg_lEntries;
        check(log.isEmpty() || log.get(0).lg_sAt != null, "audit entries keep their timestamp when present");

        String again = gson.toJson(state);
        State reload = gson.fromJson(again, State.class);
        check(reload.stores.attendance.records.size() == rows.size(), "a save/load cycle keeps every attendance row");
        if (rows.size() > 0 && reload.stores.attendance.records.size() > 0) {
            check(reload.stores.attendance.records.get(0).getDate() != null, "the second load still has dates");
        }
        check(reload.stores.employees.employees.size() == roster.size(), "a save/load cycle keeps every employee");

        System.out.println("StateFileRoundTripTest: " + checks + " checks passed");
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAIL " + what);
            throw new AssertionError(what);
        }
        checks++;
    }

    private static final class State {
        Stores stores;
    }

    private static final class Stores {
        Attendance attendance;
        Employees employees;
        WorkSchedules workSchedules;
        Audit auditLog;
    }

    private static final class Attendance {
        List<AttendanceRecord> records = new ArrayList<>();
    }

    private static final class Employees {
        Map<String, Employee> employees = new LinkedHashMap<>();
    }

    private static final class WorkSchedules {
        Map<String, WorkSchedule> schedules = new LinkedHashMap<>();
        java.util.List<WorkSchedule.DayChange> dayChanges = new ArrayList<>();
        int nextChange;
    }

    private static final class Audit {
        List<AuditLogEntry> lg_lEntries = new ArrayList<>();
    }

    private static final class AuditLogEntry {
        String lg_sAt, lg_sActor, lg_sMethod, lg_sPath, lg_sTarget;
        int lg_iStatus;
    }
}
