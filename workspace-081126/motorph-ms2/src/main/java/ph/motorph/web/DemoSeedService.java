package ph.motorph.web;

import com.google.gson.JsonObject;

import java.util.Arrays;

/** Creates only fictional, clearly marked records in one signed-in user's demo sandbox. */
public final class DemoSeedService {
    private final FirestoreRestClient client;

    public DemoSeedService(FirestoreRestClient client) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        this.client = client;
    }

    public SeedResult seedMissingSyntheticRecords() {
        // Preflight the new rules path before any writes so an unpublished ruleset cannot leave a partial seed.
        client.listDocuments(client.userCollection("demoEmployeeSchedules"));
        int employeesCreated = 0;
        for (Employee employee : new InMemoryEmployeeStore().findAll()) {
            if (client.createDocument(client.userCollection("demoEmployees"), employee.getId(), employeeFields(employee))) {
                employeesCreated++;
            }
        }

        int mappingsCreated = 0;
        EmployeeScheduleStore sampleMappings = new InMemoryEmployeeScheduleStore();
        for (Employee employee : new InMemoryEmployeeStore().findAll()) {
            java.util.OptionalInt scheduleId = sampleMappings.findScheduleIdForEmployee(employee.getId());
            if (scheduleId.isPresent() && client.createDocument(client.userCollection("demoEmployeeSchedules"),
                employee.getId(), scheduleMappingFields(employee.getId(), scheduleId.getAsInt()))) {
                mappingsCreated++;
            }
        }

        int attendanceCreated = 0;
        InMemoryAttendanceStore sampleAttendance = new InMemoryAttendanceStore();
        for (int scheduleId : Arrays.asList(101, 102, 103, 104)) {
            for (AttendanceRecord record : sampleAttendance.findByScheduleId(scheduleId)) {
                if (client.createDocument(client.userCollection("demoAttendance"),
                    Integer.toString(record.getAttendanceId()), attendanceFields(record))) {
                    attendanceCreated++;
                }
            }
        }
        return new SeedResult(employeesCreated, mappingsCreated, attendanceCreated);
    }

    private static JsonObject employeeFields(Employee employee) {
        JsonObject fields = new JsonObject();
        fields.add("id", string(employee.getId()));
        fields.add("name", string(employee.getName()));
        fields.add("department", string(employee.getDepartment()));
        fields.add("role", string(employee.getRole()));
        fields.add("location", string(employee.getLocation()));
        fields.add("status", string(employee.getStatus()));
        if (employee.getMonthlyBasic() != null) {
            fields.add("monthlyBasic", string(employee.getMonthlyBasic().toPlainString()));
            fields.add("riceSubsidy", string(employee.getRiceSubsidy().toPlainString()));
            fields.add("phoneAllowance", string(employee.getPhoneAllowance().toPlainString()));
            fields.add("clothingAllowance", string(employee.getClothingAllowance().toPlainString()));
        }
        return fields;
    }

    private static JsonObject scheduleMappingFields(String employeeId, int scheduleId) {
        JsonObject fields = new JsonObject();
        fields.add("employeeId", string(employeeId));
        fields.add("scheduleId", integer(scheduleId));
        return fields;
    }

    private static JsonObject attendanceFields(AttendanceRecord record) {
        JsonObject fields = new JsonObject();
        fields.add("attendanceId", integer(record.getAttendanceId()));
        fields.add("scheduleId", integer(record.getScheduleId()));
        fields.add("date", string(record.getDate().toString()));
        fields.add("timeIn", string(record.getTimeIn().toString()));
        // Break punches are optional on the model; only write them when the row has them.
        if (record.getBreakOut() != null) fields.add("breakOut", string(record.getBreakOut().toString()));
        if (record.getBreakIn() != null) fields.add("breakIn", string(record.getBreakIn().toString()));
        fields.add("timeOut", string(record.getTimeOut().toString()));
        fields.add("hoursWorked", string(record.getHoursWorked().toPlainString()));
        fields.add("dayType", string("DEMO"));
        return fields;
    }

    private static JsonObject string(String value) {
        JsonObject typedValue = new JsonObject();
        typedValue.addProperty("stringValue", value);
        return typedValue;
    }

    private static JsonObject integer(int value) {
        JsonObject typedValue = new JsonObject();
        typedValue.addProperty("integerValue", Integer.toString(value));
        return typedValue;
    }

    public static final class SeedResult {
        private final int employeesCreated;
        private final int scheduleMappingsCreated;
        private final int attendanceCreated;

        SeedResult(int employeesCreated, int scheduleMappingsCreated, int attendanceCreated) {
            this.employeesCreated = employeesCreated;
            this.scheduleMappingsCreated = scheduleMappingsCreated;
            this.attendanceCreated = attendanceCreated;
        }

        public int getEmployeesCreated() { return employeesCreated; }
        public int getScheduleMappingsCreated() { return scheduleMappingsCreated; }
        public int getAttendanceCreated() { return attendanceCreated; }
    }
}
