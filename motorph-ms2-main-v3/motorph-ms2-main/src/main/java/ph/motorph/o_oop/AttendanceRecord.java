package ph.motorph.o_oop;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public final class AttendanceRecord {
    @SerializedName("attendanceId") private final int ar_iId;
    @SerializedName("scheduleId") private final int ar_iSchedId;
    @SerializedName("date") private final LocalDate ar_dDate;
    @SerializedName("timeIn") private final LocalTime ar_tIn;
    @SerializedName("timeOut") private final LocalTime ar_tOut;
    @SerializedName("dayType") private final String ar_sDayType;

    public AttendanceRecord(int attendanceId, int scheduleId, LocalDate date,
                            LocalTime timeIn, LocalTime timeOut, String dayType) {
        if (attendanceId <= 0) throw new IllegalArgumentException("attendanceId must be positive");
        if (scheduleId <= 0) throw new IllegalArgumentException("scheduleId must be positive");
        this.ar_dDate = Objects.requireNonNull(date, "date must not be null");
        this.ar_tIn = Objects.requireNonNull(timeIn, "timeIn must not be null");
        this.ar_tOut = Objects.requireNonNull(timeOut, "timeOut must not be null");
        if (timeIn.isAfter(timeOut)) {
            throw new IllegalArgumentException("timeIn must be less than or equal to timeOut per the supplied data dictionary");
        }
        if (dayType == null || dayType.trim().isEmpty()) throw new IllegalArgumentException("dayType must not be blank");
        this.ar_iId = attendanceId;
        this.ar_iSchedId = scheduleId;
        this.ar_sDayType = dayType.trim();
    }

    public int getAttendanceId() { return ar_iId; }
    public int getScheduleId() { return ar_iSchedId; }
    public LocalDate getDate() { return ar_dDate; }
    public LocalTime getTimeIn() { return ar_tIn; }
    public LocalTime getTimeOut() { return ar_tOut; }
    public String getDayType() { return ar_sDayType; }
}
