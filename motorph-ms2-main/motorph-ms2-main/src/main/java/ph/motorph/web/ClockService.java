package ph.motorph.web;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Clock in / clock out for the signed-in employee. Clock in opens a shift (held here, in memory); clock out turns it
 * into one raw attendance row with the server's time, to the minute. One attendance row per person per day, as before.
 */
public final class ClockService {
    static final int QR_MINUTES = 10;
    private static final String QR_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private final Clock clock;
    private final Map<String, LocalDateTime> openShifts = new HashMap<>();
    // Requirement 2: clocking in from the app counts as offsite until the shift's QR code is scanned at the branch.
    private final Map<String, String> onsiteCheckIns = new HashMap<>(); // "employeeId|date" -> HH:mm scanned
    private final Map<String, String> qrCodes = new HashMap<>();        // employeeId -> code (not saved; a new one is shown)
    private final Map<String, LocalDateTime> qrExpires = new HashMap<>();

    public ClockService(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("clock must not be null");
        this.clock = clock;
    }

    public LocalDateTime now() { return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES); }

    /** Open shifts for the data file. */
    public synchronized Map<String, LocalDateTime> exportOpenShifts() { return new HashMap<>(openShifts); }

    public synchronized void importOpenShifts(Map<String, LocalDateTime> shifts) {
        openShifts.clear();
        if (shifts != null) openShifts.putAll(shifts);
        qrCodes.clear();
        qrExpires.clear();
    }

    public synchronized Map<String, String> exportOnsiteCheckIns() { return new HashMap<>(onsiteCheckIns); }

    public synchronized void importOnsiteCheckIns(Map<String, String> checkIns) {
        onsiteCheckIns.clear();
        if (checkIns != null) onsiteCheckIns.putAll(checkIns);
    }

    /** When this person scanned in onsite on that day, if they did. */
    public synchronized Optional<LocalTime> onsiteAt(String employeeId, LocalDate date) {
        String time = onsiteCheckIns.get(employeeId + "|" + date);
        return time == null ? Optional.empty() : Optional.of(LocalTime.parse(time));
    }

    /** The open shift's QR code and when it expires, while the person is clocked in and not yet onsite. */
    public synchronized Optional<String> currentQr(String employeeId) {
        if (!qrCodes.containsKey(employeeId) || now().isAfter(qrExpires.get(employeeId))) return Optional.empty();
        return Optional.of(qrCodes.get(employeeId));
    }

    public synchronized Optional<LocalDateTime> qrExpiry(String employeeId) { return Optional.ofNullable(qrExpires.get(employeeId)); }

    /** A new QR code for the open shift (the old one stops working). */
    public synchronized String issueQr(String employeeId) {
        LocalDateTime start = openShifts.get(employeeId);
        if (start == null) throw new IllegalStateException("Clock in first.");
        if (onsiteAt(employeeId, start.toLocalDate()).isPresent()) throw new IllegalStateException("You are already checked in onsite.");
        StringBuilder code = new StringBuilder("MPH-");
        for (int i = 0; i < 6; i++) code.append(QR_LETTERS.charAt(RANDOM.nextInt(QR_LETTERS.length())));
        qrCodes.put(employeeId, code.toString());
        qrExpires.put(employeeId, now().plusMinutes(QR_MINUTES));
        return code.toString();
    }

    /** The branch scanner read this code: the shift becomes onsite at the scan time. */
    public synchronized LocalTime scanOnsite(String employeeId, String code) {
        LocalDateTime start = openShifts.get(employeeId);
        if (start == null) throw new IllegalStateException("Clock in first.");
        if (onsiteAt(employeeId, start.toLocalDate()).isPresent()) throw new IllegalStateException("You are already checked in onsite.");
        if (!currentQr(employeeId).isPresent()) throw new IllegalStateException("That QR code expired; show a new one.");
        if (!qrCodes.get(employeeId).equals(code == null ? "" : code.trim().toUpperCase())) throw new IllegalArgumentException("That QR code is not for this shift.");
        LocalTime scanned = now().toLocalTime();
        onsiteCheckIns.put(employeeId + "|" + start.toLocalDate(), scanned.toString());
        qrCodes.remove(employeeId);
        qrExpires.remove(employeeId);
        return scanned;
    }

    public synchronized Optional<LocalDateTime> openShift(String employeeId) {
        return Optional.ofNullable(openShifts.get(employeeId));
    }

    public synchronized LocalDateTime clockIn(AttendanceService attendance, String employeeId) {
        if (openShifts.containsKey(employeeId)) throw new IllegalStateException("You are already clocked in.");
        LocalDateTime now = now();
        // Also checks that the employee exists; unmapped employees are refused at clock out by createForEmployee.
        if (!attendance.findForEmployee(employeeId, now.toLocalDate(), now.toLocalDate()).isEmpty()) {
            throw new IllegalStateException("Attendance is already recorded for today.");
        }
        openShifts.put(employeeId, now);
        issueQr(employeeId);
        return now;
    }

    public synchronized AttendanceRecord clockOut(AttendanceService attendance, String employeeId) {
        LocalDateTime start = openShifts.get(employeeId);
        if (start == null) throw new IllegalStateException("You are not clocked in.");
        LocalDateTime now = now();
        LocalDate date = start.toLocalDate();
        // shortcut: a shift past midnight is closed at 23:59 of the start day; overnight shifts need a two-day rule.
        LocalTime out = now.toLocalDate().equals(date) ? now.toLocalTime() : LocalTime.of(23, 59);
        AttendanceRecord record = attendance.createForEmployee(employeeId, date, start.toLocalTime(), out);
        openShifts.remove(employeeId);
        qrCodes.remove(employeeId);
        qrExpires.remove(employeeId);
        return record;
    }
}
