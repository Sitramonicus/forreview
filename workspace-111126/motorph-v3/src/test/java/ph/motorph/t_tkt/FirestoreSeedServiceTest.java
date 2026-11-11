package ph.motorph.t_tkt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import ph.motorph.o_oop.StateStore;

/**
 * Guards the two ways a "copy my data to Firestore" feature can quietly destroy data:
 *
 *  - uploading the wrong side (the remote one) leaves the local file empty, or
 *  - uploading whatever happens to be in memory writes an empty store over a full one.
 *
 * Both are checked against an in-memory StateStore, so this runs with no network.
 */
public final class FirestoreSeedServiceTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        uploadsTheLocalSideToTheRemoteOne();
        downloadingReplacesTheLocalSide();
        refusesToUploadNothing();
        refusesToDownloadFromAnEmptyStore();
        reportsCountsForTheWholeDataset();
        System.out.println("PASS: " + checks + " Firestore seed-service checks");
    }

    private static void uploadsTheLocalSideToTheRemoteOne() throws IOException {
        MemoryStore local = MemoryStore.with(34, 7421);
        MemoryStore remote = MemoryStore.empty();
        FirestoreSeedService.UploadResult result = new FirestoreSeedService(local, remote).uploadAll();
        check(remote.employees() == 34, "upload leaves 34 employees on the remote side, not " + remote.employees());
        check(remote.rows() == 7421, "upload leaves 7,421 attendance rows on the remote side, not " + remote.rows());
        check(local.employees() == 34, "upload does not disturb the local side");
        check(result.getEmployeeCount() == 34 && result.getAttendanceCount() == 7421,
            "upload reports what it moved");
    }

    private static void downloadingReplacesTheLocalSide() throws IOException {
        MemoryStore local = MemoryStore.with(1, 2);
        MemoryStore remote = MemoryStore.with(34, 7421);
        FirestoreSeedService.UploadResult result = new FirestoreSeedService(local, remote).downloadAll();
        check(local.employees() == 34 && local.rows() == 7421,
            "download replaces the local side with the remote one (got " + local.employees() + "/" + local.rows() + ")");
        check(remote.employees() == 34, "download does not disturb the remote side");
        check(result.getAttendanceCount() == 7421, "download reports what it moved");
    }

    private static void refusesToUploadNothing() {
        MemoryStore local = MemoryStore.empty();
        MemoryStore remote = MemoryStore.empty();
        try {
            new FirestoreSeedService(local, remote).uploadAll();
            check(false, "uploading an empty local store must fail loudly");
        } catch (IllegalStateException ex) {
            check(true, "uploading an empty local store fails instead of blanking the remote one");
        } catch (IOException ex) {
            check(false, "uploading empty should raise IllegalStateException, not " + ex);
        }
    }

    private static void refusesToDownloadFromAnEmptyStore() {
        MemoryStore local = MemoryStore.with(34, 7421);
        MemoryStore remote = MemoryStore.empty();
        try {
            new FirestoreSeedService(local, remote).downloadAll();
            check(false, "downloading from an empty remote store must fail loudly");
        } catch (IllegalStateException ex) {
            check(true, "downloading from an empty remote store fails instead of wiping the local file");
        } catch (IOException ex) {
            check(false, "download from empty should raise IllegalStateException, not " + ex);
        }
        check(local.rows() == 7421, "a refused download leaves the local data untouched");
    }

    private static void reportsCountsForTheWholeDataset() throws IOException {
        MemoryStore local = MemoryStore.with(34, 7421);
        MemoryStore remote = MemoryStore.empty();
        new FirestoreSeedService(local, remote).uploadAll();
        check(remote.load().get().has("attendance"), "the attendance store reaches the remote side at all");
        check(remote.load().get().getAsJsonObject("attendance").getAsJsonArray("records").size() == 7421,
            "all 7,421 rows survive the move, not a sample");
    }

    /** The service only needs load()/save(); a plain in-memory StateStore is enough. */
    private static final class MemoryStore implements StateStore {
        private JsonObject state;

        static MemoryStore empty() { return new MemoryStore(null); }

        static MemoryStore with(int employees, int rows) {
            JsonObject state = new JsonObject();

            JsonObject employeeStore = new JsonObject();
            JsonObject roster = new JsonObject();
            for (int i = 1; i <= employees; i++) {
                JsonObject person = new JsonObject();
                person.addProperty("id", String.format("100%02d", i));
                roster.add(String.format("100%02d", i), person);
            }
            employeeStore.add("employees", roster);
            state.add("employees", employeeStore);

            JsonObject attendanceStore = new JsonObject();
            JsonArray records = new JsonArray();
            for (int i = 0; i < rows; i++) {
                JsonObject row = new JsonObject();
                row.addProperty("employeeId", "10001");
                row.addProperty("date", "2026-01-01");
                records.add(row);
            }
            attendanceStore.add("records", records);
            state.add("attendance", attendanceStore);
            return new MemoryStore(state);
        }

        private MemoryStore(JsonObject state) { this.state = state; }

        @Override public java.util.Optional<JsonObject> load() {
            return java.util.Optional.ofNullable(state == null ? null : state.deepCopy());
        }

        @Override public void save(JsonObject stores) { this.state = stores.deepCopy(); }

        @Override public com.google.gson.Gson gson() { return new com.google.gson.Gson(); }

        @Override public String describe() { return "memory"; }

        @Override public String backend() { return "memory"; }

        int employees() {
            if (state == null || !state.has("employees")) return 0;
            return state.getAsJsonObject("employees").getAsJsonObject("employees").size();
        }

        int rows() {
            if (state == null || !state.has("attendance")) return 0;
            return state.getAsJsonObject("attendance").getAsJsonArray("records").size();
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAIL " + what);
            System.exit(1);
        }
        checks++;
    }
}
