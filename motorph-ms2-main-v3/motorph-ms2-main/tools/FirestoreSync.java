import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.nio.file.Paths;
import ph.motorph.o_oop.FirestoreRestClient;
import ph.motorph.o_oop.FirestoreStateStore;
import ph.motorph.o_oop.LocalStateFile;
import ph.motorph.o_oop.StateStore;
import ph.motorph.t_tkt.FirestoreSeedService;

/**
 * Moves the whole state document between data/local-demo.json and Firestore.
 *
 *   java tools/FirestoreSync.java upload UPLOAD     copy the local file up
 *   java tools/FirestoreSync.java download REPLACE  replace the local file from Firestore
 *   java tools/FirestoreSync.java diff              show what each side holds, change nothing
 *
 * Requires FIREBASE_PROJECT_ID and Application Default Credentials. An optional
 * MOTORPH_FIRESTORE_TOKEN overrides ADC but does not refresh. Run with the app
 * classpath as documented in docs/firebase-setup-guide.md.
 */
public final class FirestoreSync {

    public static void main(String[] args) throws Exception {
        String action = args.length == 0 ? "diff" : args[0].toLowerCase(java.util.Locale.ROOT);
        if ("upload".equals(action) && (args.length < 2 || !"UPLOAD".equals(args[1]))) {
            System.err.println("Upload replaces Firestore. Stop the server, back up first, and pass UPLOAD to confirm.");
            System.exit(2);
        }
        if ("download".equals(action) && (args.length < 2 || !"REPLACE".equals(args[1]))) {
            System.err.println("Download replaces the local file. Back up first, and pass REPLACE to confirm.");
            System.exit(2);
        }
        String projectId = env("FIREBASE_PROJECT_ID");
        String token = env("MOTORPH_FIRESTORE_TOKEN");
        String uid = env("MOTORPH_FIRESTORE_UID");
        Path localPath = Paths.get(System.getenv().getOrDefault("LOCAL_DATA_FILE", "data/local-demo.json"));

        if (projectId.isEmpty()) {
            System.err.println("Set FIREBASE_PROJECT_ID and configure Application Default Credentials first.");
            System.exit(2);
        }
        if (uid.isEmpty()) uid = "server";
        StateStore local = new LocalStateFile(localPath);
        FirestoreRestClient client = token.isEmpty()
            ? FirestoreRestClient.applicationDefault(projectId, uid, null)
            : new FirestoreRestClient(projectId, uid, token);
        StateStore remote = new FirestoreStateStore(client);

        switch (action) {
            case "diff":
                report("local", local);
                report("firestore", remote);
                break;
            case "upload": {
                FirestoreSeedService.UploadResult result = new FirestoreSeedService(local, remote).uploadAll();
                System.out.println("Uploaded " + result.getEmployeeCount() + " employees and "
                    + result.getAttendanceCount() + " attendance rows to Firestore.");
                break;
            }
            case "download": {
                FirestoreSeedService.UploadResult result = new FirestoreSeedService(local, remote).downloadAll();
                System.out.println("Replaced the local file with " + result.getEmployeeCount() + " employees and "
                    + result.getAttendanceCount() + " attendance rows from Firestore.");
                break;
            }
            default:
                System.out.println("Usage: java tools/FirestoreSync.java [diff|upload UPLOAD|download REPLACE]");
                System.exit(1);
        }
    }

    private static void report(String label, StateStore store) throws Exception {
        java.util.Optional<JsonObject> state = store.load();
        if (state.isEmpty()) {
            System.out.printf("%-10s empty%n", label);
            return;
        }
        JsonObject stores = state.get();
        int employees = 0, attendance = 0;
        if (stores.has("employees") && stores.getAsJsonObject("employees").has("employees")) {
            employees = stores.getAsJsonObject("employees").getAsJsonObject("employees").size();
        }
        if (stores.has("attendance") && stores.getAsJsonObject("attendance").has("records")) {
            attendance = stores.getAsJsonObject("attendance").getAsJsonArray("records").size();
        }
        System.out.printf("%-10s %d employees, %d attendance rows, %d stores%n", label, employees, attendance, stores.size());
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }
}
