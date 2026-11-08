package ph.motorph.t_tkt;

import ph.motorph.o_oop.AttendanceRecord;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.FirestoreRestClient;
import ph.motorph.o_oop.InMemoryAttendanceStore;
import ph.motorph.o_oop.InMemoryEmployeeStore;
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
        int employeesCreated = 0;
        for (Employee employee : new InMemoryEmployeeStore().findAll()) {
            if (client.createDocument(client.userCollection("demoEmployees"), employee.getId(), employeeFields(employee))) {
                employeesCreated++;
            }
        }

        int attendanceCreated = 0;
        InMemoryAttendanceStore sampleAttendance = new InMemoryAttendanceStore();
        for (int scheduleId : Arrays.asList(101, 102, 103)) {
            for (AttendanceRecord record : sampleAttendance.findByScheduleId(scheduleId)) {
                if (client.createDocument(client.userCollection("demoAttendance"),
                    Integer.toString(record.getAttendanceId()), attendanceFields(record))) {
                    attendanceCreated++;
                }
            }
        }
        return new SeedResult(employeesCreated, attendanceCreated);
    }

    private static JsonObject employeeFields(Employee employee) {
        JsonObject fields = new JsonObject();
        fields.add("id", string(employee.getId()));
        fields.add("name", string(employee.getName()));
        fields.add("department", string(employee.getDepartment()));
        fields.add("role", string(employee.getRole()));
        fields.add("location", string(employee.getLocation()));
        fields.add("status", string(employee.getStatus()));
        return fields;
    }

    private static JsonObject attendanceFields(AttendanceRecord record) {
        JsonObject fields = new JsonObject();
        fields.add("attendanceId", integer(record.getAttendanceId()));
        fields.add("scheduleId", integer(record.getScheduleId()));
        fields.add("date", string(record.getDate().toString()));
        fields.add("timeIn", string(record.getTimeIn().toString()));
        fields.add("timeOut", string(record.getTimeOut().toString()));
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
        private final int attendanceCreated;

        SeedResult(int employeesCreated, int attendanceCreated) {
            this.employeesCreated = employeesCreated;
            this.attendanceCreated = attendanceCreated;
        }

        public int getEmployeesCreated() { return employeesCreated; }
        public int getAttendanceCreated() { return attendanceCreated; }
    }
}
