package ph.motorph.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

/** Reads owner-scoped synthetic employee/schedule links from Firestore; it has no in-code assignment fallback. */
public final class FirestoreEmployeeScheduleStore implements EmployeeScheduleStore {
    private final FirestoreRestClient client;
    private volatile Map<String, Integer> scheduleByEmployee;

    public FirestoreEmployeeScheduleStore(FirestoreRestClient client) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        this.client = client;
    }

    @Override
    public OptionalInt findScheduleIdForEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) return OptionalInt.empty();
        Integer scheduleId = mappings().get(employeeId.trim());
        return scheduleId == null ? OptionalInt.empty() : OptionalInt.of(scheduleId);
    }

    private Map<String, Integer> mappings() {
        Map<String, Integer> current = scheduleByEmployee;
        if (current != null) return current;
        synchronized (this) {
            if (scheduleByEmployee != null) return scheduleByEmployee;
            Map<String, Integer> loaded = new LinkedHashMap<>();
            for (JsonObject document : client.listDocuments(client.userCollection("demoEmployeeSchedules"))) {
                JsonObject fields = document.has("fields") ? document.getAsJsonObject("fields") : new JsonObject();
                String employeeId = stringValue(fields, "employeeId");
                int scheduleId = integerValue(fields, "scheduleId");
                if (!employeeId.isEmpty() && scheduleId > 0) loaded.put(employeeId, scheduleId);
            }
            scheduleByEmployee = java.util.Collections.unmodifiableMap(loaded);
            return scheduleByEmployee;
        }
    }

    private static String stringValue(JsonObject fields, String name) {
        JsonElement element = fields.get(name);
        if (element == null || !element.isJsonObject()) return "";
        JsonElement value = element.getAsJsonObject().get("stringValue");
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static int integerValue(JsonObject fields, String name) {
        JsonElement element = fields.get(name);
        if (element == null || !element.isJsonObject()) return 0;
        JsonElement value = element.getAsJsonObject().get("integerValue");
        if (value == null || value.isJsonNull()) return 0;
        try { return Integer.parseInt(value.getAsString()); }
        catch (NumberFormatException ignored) { return 0; }
    }
}
