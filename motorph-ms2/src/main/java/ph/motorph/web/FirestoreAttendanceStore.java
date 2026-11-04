package ph.motorph.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** User-scoped synthetic attendance records read through Firestore REST. */
public final class FirestoreAttendanceStore implements AttendanceStore {
    private final FirestoreRestClient client;

    public FirestoreAttendanceStore(FirestoreRestClient client) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        this.client = client;
    }

    @Override
    public List<AttendanceRecord> findAll() {
        List<AttendanceRecord> records = new ArrayList<>();
        for (JsonObject document : client.listDocuments(client.userCollection("demoAttendance"))) {
            records.add(fromDocument(document));
        }
        return records;
    }

    @Override
    public List<AttendanceRecord> findByScheduleId(int scheduleId) {
        return findAll().stream().filter(record -> record.getScheduleId() == scheduleId).collect(Collectors.toList());
    }

    private static AttendanceRecord fromDocument(JsonObject document) {
        JsonObject fields = document.has("fields") ? document.getAsJsonObject("fields") : new JsonObject();
        return new AttendanceRecord(
            integerValue(fields, "attendanceId"), integerValue(fields, "scheduleId"),
            LocalDate.parse(stringValue(fields, "date")), LocalTime.parse(stringValue(fields, "timeIn")),
            LocalTime.parse(stringValue(fields, "timeOut")), stringValue(fields, "dayType"));
    }

    private static int integerValue(JsonObject fields, String name) {
        JsonElement element = fields.get(name);
        if (element == null || !element.isJsonObject()) return 0;
        JsonElement value = element.getAsJsonObject().get("integerValue");
        return value == null || value.isJsonNull() ? 0 : Integer.parseInt(value.getAsString());
    }

    private static String stringValue(JsonObject fields, String name) {
        return FirestoreEmployeeStore.stringValue(fields, name);
    }
}
