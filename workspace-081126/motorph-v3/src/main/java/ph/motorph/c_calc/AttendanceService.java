package ph.motorph.c_calc;

import ph.motorph.o_oop.AttendanceRecord;
import ph.motorph.o_oop.AttendanceStore;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeScheduleStore;
import ph.motorph.o_oop.EmployeeService;
import ph.motorph.o_oop.MutableAttendanceStore;
import ph.motorph.o_oop.RawAttendanceSummary;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/** Reads raw attendance for one employee; deliberately does not calculate paid hours. */
public final class AttendanceService {
    private final EmployeeService employeeService;
    private final EmployeeScheduleStore scheduleStore;
    private final AttendanceStore attendanceStore;

    public AttendanceService(EmployeeService employeeService, EmployeeScheduleStore scheduleStore,
                             AttendanceStore attendanceStore) {
        if (employeeService == null || scheduleStore == null || attendanceStore == null) {
            throw new IllegalArgumentException("attendance service dependencies must not be null");
        }
        this.employeeService = employeeService;
        this.scheduleStore = scheduleStore;
        this.attendanceStore = attendanceStore;
    }

    /** Inclusive optional date filtering; no pay-period calendar is inferred. */
    public List<AttendanceRecord> findForEmployee(String employeeId, LocalDate from, LocalDate to) {
        if (!employeeService.findEmployee(employeeId).isPresent()) {
            throw new NoSuchElementException("employee not found");
        }
        validateRange(from, to);
        OptionalInt scheduleId = scheduleStore.findScheduleIdForEmployee(employeeId.trim());
        if (!scheduleId.isPresent()) return java.util.Collections.emptyList();
        return attendanceStore.findByScheduleId(scheduleId.getAsInt()).stream()
            .filter(record -> from == null || !record.getDate().isBefore(from))
            .filter(record -> to == null || !record.getDate().isAfter(to))
            .collect(Collectors.toList());
    }

    /** Adds one local synthetic raw row for an existing mapping; no hours or classifications are inferred. */
    public AttendanceRecord createForEmployee(String employeeId, LocalDate date, LocalTime timeIn, LocalTime timeOut) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeId is required.");
        }
        if (!employeeService.findEmployee(employeeId.trim()).isPresent()) {
            throw new NoSuchElementException("employee not found");
        }
        OptionalInt scheduleId = scheduleStore.findScheduleIdForEmployee(employeeId.trim());
        if (!scheduleId.isPresent()) {
            throw new IllegalStateException("No configured synthetic schedule mapping exists for this employee; no schedule was assigned.");
        }
        if (!(attendanceStore instanceof MutableAttendanceStore)) {
            throw new IllegalStateException("Attendance entry is disabled for this storage mode.");
        }
        return ((MutableAttendanceStore) attendanceStore).create(scheduleId.getAsInt(), date, timeIn, timeOut, "DEMO");
    }

    /** Corrects the times of one of this employee's rows; returns {original, corrected}. */
    public AttendanceRecord[] correctForEmployee(String employeeId, int attendanceId, LocalTime timeIn, LocalTime timeOut) {
        if (timeIn.isAfter(timeOut)) throw new IllegalArgumentException("Time in must be on or before time out.");
        AttendanceRecord original = findForEmployee(employeeId, null, null).stream()
            .filter(record -> record.getAttendanceId() == attendanceId).findFirst()
            .orElseThrow(() -> new NoSuchElementException("That attendance row does not belong to this employee."));
        if (!(attendanceStore instanceof MutableAttendanceStore)) throw new IllegalStateException("Attendance edits are disabled for this storage mode.");
        return new AttendanceRecord[] {original, ((MutableAttendanceStore) attendanceStore).correctTimes(attendanceId, timeIn, timeOut)};
    }

    /** Counts raw rows and their date coverage; this never classifies attendance or derives hours. */
    public List<RawAttendanceSummary> summarizeRawRecords(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<AttendanceRecord> allRecords = attendanceStore.findAll();
        List<RawAttendanceSummary> summaries = new ArrayList<>();
        for (Employee employee : employeeService.listEmployees()) {
            OptionalInt schedule = scheduleStore.findScheduleIdForEmployee(employee.getId());
            if (!schedule.isPresent()) {
                summaries.add(new RawAttendanceSummary(employee.getId(), employee.getName(), null, 0, "", ""));
                continue;
            }
            List<AttendanceRecord> matched = allRecords.stream()
                .filter(record -> record.getScheduleId() == schedule.getAsInt())
                .filter(record -> from == null || !record.getDate().isBefore(from))
                .filter(record -> to == null || !record.getDate().isAfter(to))
                .collect(Collectors.toList());
            String firstDate = matched.stream().map(record -> record.getDate().toString()).min(String::compareTo).orElse("");
            String lastDate = matched.stream().map(record -> record.getDate().toString()).max(String::compareTo).orElse("");
            summaries.add(new RawAttendanceSummary(employee.getId(), employee.getName(), schedule.getAsInt(),
                matched.size(), firstDate, lastDate));
        }
        summaries.sort(Comparator.comparing(RawAttendanceSummary::getEmployeeId));
        return summaries;
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from date must be on or before to date");
        }
    }
}
