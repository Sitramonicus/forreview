package ph.motorph.t_tkt;

import com.google.gson.JsonObject;
import ph.motorph.o_oop.StateStore;

/**
 * Pushes the server's current state to Firestore, or copies Firestore's state back.
 *
 * This used to write 34 stub employees and 19 sample attendance rows into a per-user
 * collection, which is not the dataset the payroll actually runs on. It now moves the
 * whole state document, so what lands in Firestore is the same 7,421 attendance rows and
 * 34 employee records the app is using.
 */
public final class FirestoreSeedService {

    private final StateStore local;
    private final StateStore remote;

    /** Upload reads {@code local} and writes {@code remote}; download does the reverse. */
    public FirestoreSeedService(StateStore local, StateStore remote) {
        this.local = local;
        this.remote = remote;
    }

    /** Copy everything this server holds into the target store. */
    public UploadResult uploadAll() throws java.io.IOException {
        java.util.Optional<JsonObject> mine = local.load();
        if (mine.isEmpty()) throw new IllegalStateException("There is no local state to upload yet.");
        JsonObject stores = mine.get();
        remote.save(stores);
        return new UploadResult(employees(stores), attendanceRows(stores));
    }

    /** Replace this server's state with whatever the target store holds. */
    public UploadResult downloadAll() throws java.io.IOException {
        java.util.Optional<JsonObject> theirs = remote.load();
        if (theirs.isEmpty()) throw new IllegalStateException("The remote store is empty; nothing to download.");
        JsonObject stores = theirs.get();
        local.save(stores);
        return new UploadResult(employees(stores), attendanceRows(stores));
    }

    private static int employees(JsonObject stores) {
        if (!stores.has("employees")) return 0;
        JsonObject store = stores.getAsJsonObject("employees");
        if (!store.has("employees")) return 0;
        return store.getAsJsonObject("employees").size();
    }

    private static int attendanceRows(JsonObject stores) {
        if (!stores.has("attendance")) return 0;
        JsonObject store = stores.getAsJsonObject("attendance");
        if (!store.has("records")) return 0;
        return store.getAsJsonArray("records").size();
    }

    public static final class UploadResult {
        private final int employeeCount, attendanceCount;

        UploadResult(int employeeCount, int attendanceCount) {
            this.employeeCount = employeeCount;
            this.attendanceCount = attendanceCount;
        }

        public int getEmployeeCount() { return employeeCount; }
        public int getAttendanceCount() { return attendanceCount; }
    }
}
