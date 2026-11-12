package ph.motorph.c_calc;

import ph.motorph.o_oop.AttendanceRecord;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClockService {
    static final int qr_iMinutes = 10;
    private static final String qr_sLetters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final java.security.SecureRandom qr_rnd = new java.security.SecureRandom();
    private final Clock qr_clk;
    private final Map<String, LocalDateTime> qr_mShifts = new HashMap<>();
    
    private final Map<String, String> qr_mOnsite = new HashMap<>(); 
    private final Map<String, String> qr_mCodes = new HashMap<>();       
    private final Map<String, LocalDateTime> qr_mExpires = new HashMap<>();

    public ClockService(Clock qr_clkIn) {
        if (qr_clkIn == null) throw new IllegalArgumentException("clock must not be null");
        this.qr_clk = qr_clkIn;
    }

    public LocalDateTime now() { return LocalDateTime.now(qr_clk).truncatedTo(ChronoUnit.MINUTES); }

    
    public synchronized Map<String, LocalDateTime> exportOpenShifts() { return new HashMap<>(qr_mShifts); }

    public synchronized void importOpenShifts(Map<String, LocalDateTime> qr_mIn) {
        qr_mShifts.clear();
        if (qr_mIn != null) qr_mShifts.putAll(qr_mIn);
        qr_mCodes.clear();
        qr_mExpires.clear();
    }

    public synchronized Map<String, String> exportOnsiteCheckIns() { return new HashMap<>(qr_mOnsite); }

    public synchronized void importOnsiteCheckIns(Map<String, String> qr_mIn) {
        qr_mOnsite.clear();
        if (qr_mIn != null) qr_mOnsite.putAll(qr_mIn);
    }

    
    public synchronized Optional<LocalTime> onsiteAt(String qr_sEid, LocalDate qr_dDate) {
        String qr_sTime = qr_mOnsite.get(qr_sEid + "|" + qr_dDate);
        return qr_sTime == null ? Optional.empty() : Optional.of(LocalTime.parse(qr_sTime));
    }

    
    public synchronized Optional<String> currentQr(String qr_sEid) {
        if (!qr_mCodes.containsKey(qr_sEid) || now().isAfter(qr_mExpires.get(qr_sEid))) return Optional.empty();
        return Optional.of(qr_mCodes.get(qr_sEid));
    }

    public synchronized Optional<LocalDateTime> qrExpiry(String qr_sEid) { return Optional.ofNullable(qr_mExpires.get(qr_sEid)); }

    
    public synchronized String issueQr(String qr_sEid) {
        LocalDateTime qr_dtStart = qr_mShifts.get(qr_sEid);
        if (qr_dtStart == null) throw new IllegalStateException("Clock in first.");
        if (onsiteAt(qr_sEid, qr_dtStart.toLocalDate()).isPresent()) throw new IllegalStateException("You are already checked in onsite.");
        StringBuilder qr_sbCode = new StringBuilder("MPH-");
        for (int qr_i = 0; qr_i < 6; qr_i++) qr_sbCode.append(qr_sLetters.charAt(qr_rnd.nextInt(qr_sLetters.length())));
        qr_mCodes.put(qr_sEid, qr_sbCode.toString());
        qr_mExpires.put(qr_sEid, now().plusMinutes(qr_iMinutes));
        return qr_sbCode.toString();
    }

    
    public synchronized LocalTime scanOnsite(String qr_sEid, String qr_sCode) {
        LocalDateTime qr_dtStart = qr_mShifts.get(qr_sEid);
        if (qr_dtStart == null) throw new IllegalStateException("Clock in first.");
        if (onsiteAt(qr_sEid, qr_dtStart.toLocalDate()).isPresent()) throw new IllegalStateException("You are already checked in onsite.");
        if (!currentQr(qr_sEid).isPresent()) throw new IllegalStateException("That QR code expired; show a new one.");
        if (!qr_mCodes.get(qr_sEid).equals(qr_sCode == null ? "" : qr_sCode.trim().toUpperCase(java.util.Locale.ROOT))) throw new IllegalArgumentException("That QR code is not for this shift.");
        LocalTime qr_tScanned = now().toLocalTime();
        qr_mOnsite.put(qr_sEid + "|" + qr_dtStart.toLocalDate(), qr_tScanned.toString());
        qr_mCodes.remove(qr_sEid);
        qr_mExpires.remove(qr_sEid);
        return qr_tScanned;
    }

    public synchronized Optional<LocalDateTime> openShift(String qr_sEid) {
        return Optional.ofNullable(qr_mShifts.get(qr_sEid));
    }

    public synchronized LocalDateTime clockIn(AttendanceService attendance, String qr_sEid) {
        if (qr_mShifts.containsKey(qr_sEid)) throw new IllegalStateException("You are already clocked in.");
        LocalDateTime qr_dtNow = now();
        if (!attendance.findForEmployee(qr_sEid, qr_dtNow.toLocalDate(), qr_dtNow.toLocalDate()).isEmpty()) {
            throw new IllegalStateException("Attendance is already recorded for today.");
        }
        qr_mShifts.put(qr_sEid, qr_dtNow);
        issueQr(qr_sEid);
        return qr_dtNow;
    }

    public synchronized AttendanceRecord clockOut(AttendanceService attendance, String qr_sEid) {
        LocalDateTime qr_dtStart = qr_mShifts.get(qr_sEid);
        if (qr_dtStart == null) throw new IllegalStateException("You are not clocked in.");
        LocalDateTime qr_dtNow = now();
        LocalDate qr_dDate = qr_dtStart.toLocalDate();
        LocalTime qr_tOut = qr_dtNow.toLocalDate().equals(qr_dDate) ? qr_dtNow.toLocalTime() : LocalTime.of(23, 59);
        AttendanceRecord qr_rRec = attendance.createForEmployee(qr_sEid, qr_dDate, qr_dtStart.toLocalTime(), qr_tOut);
        qr_mShifts.remove(qr_sEid);
        qr_mCodes.remove(qr_sEid);
        qr_mExpires.remove(qr_sEid);
        return qr_rRec;
    }
}
