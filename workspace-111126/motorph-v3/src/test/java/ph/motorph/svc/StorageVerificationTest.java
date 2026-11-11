package ph.motorph.svc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import ph.motorph.o_oop.LocalStateFile;
import ph.motorph.o_oop.StateStore;
import ph.motorph.sp_secpol.AuthService;
import ph.motorph.sp_secpol.SessionCookie;
import ph.motorph.sp_secpol.SessionStore;

/** Route-level tests for the admin storage status and guarded round-trip verifier. */
public final class StorageVerificationTest {
    private static final class ControlledStore implements StateStore {
        final Gson gson = new LocalStateFile(Path.of("unused-storage-test.json")).gson();
        JsonObject state;
        boolean failNextLoad;
        boolean failNextSave;
        boolean failReadBackAfterSave;
        int saveCalls;
        int loadCalls;

        public synchronized Optional<JsonObject> load() throws IOException {
            loadCalls++;
            if (failNextLoad) {
                failNextLoad = false;
                throw new IOException("controlled load failure");
            }
            if (state == null) return Optional.empty();
            return Optional.of(state.deepCopy());
        }
        public synchronized void save(JsonObject stores) throws IOException {
            saveCalls++;
            if (failNextSave) {
                failNextSave = false;
                throw new IOException("controlled save failure");
            }
            state = stores.deepCopy();
            if (failReadBackAfterSave) {
                // The next load is the verifier's immediate read-back.
                failReadBackAfterSave = false;
                failNextLoad = true;
            }
        }
        public Gson gson() { return gson; }
        public String describe() { return "controlled storage verification test store"; }
        public String backend() { return "file"; }
    }

    private static final class RunningApp implements AutoCloseable {
        final Path scratch;
        final AppServer app;
        final ControlledStore store;
        final HttpClient client;
        final URI base;
        final String sessionToken;
        final String csrf;

        RunningApp() throws Exception {
            scratch = Files.createTempDirectory("motorph-storage-verification-");
            app = new AppServer("", Path.of("web"), null, null);
            store = new ControlledStore();
            store.state = app.exportState();
            Field dataFile = AppServer.class.getDeclaredField("dataFile");
            dataFile.setAccessible(true);
            dataFile.set(app, store);
            int port = app.start("127.0.0.1", 0).getAddress().getPort();
            base = URI.create("http://127.0.0.1:" + port);
            client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

            Field sessionsField = AppServer.class.getDeclaredField("sessionStore");
            sessionsField.setAccessible(true);
            SessionStore sessions = (SessionStore) sessionsField.get(app);
            String initialSession = sessions.issue("10005");
            Field authField = AppServer.class.getDeclaredField("authService");
            authField.setAccessible(true);
            AuthService auth = (AuthService) authField.get(app);
            sessionToken = auth.changePassword(initialSession, "MotorPH-demo-2026", "Storage!Safe-2026-A");
            if (sessionToken == null) throw new AssertionError("test admin password change failed");
            csrf = SessionCookie.newCsrfToken();
            store.state = app.exportState();
        }

        private String cookieHeader() {
            return "motorph_session=" + sessionToken + "; motorph_csrf=" + csrf;
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(base.resolve(path)).header("Cookie", cookieHeader()).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> rawPost(String path, String body, boolean withCsrf) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path)).header("Cookie", cookieHeader())
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
            if (withCsrf) request.header("X-CSRF-Token", csrf);
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        void resetBrokenFlag() throws Exception {
            Field broken = AppServer.class.getDeclaredField("persistenceBroken");
            broken.setAccessible(true);
            broken.setBoolean(app, false);
        }

        public void close() throws Exception {
            app.stop();
            try (java.util.stream.Stream<Path> files = Files.walk(scratch)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            }
        }
    }

    private static final class Response {
        final HttpResponse<String> response;
        Response(HttpResponse<String> response) { this.response = response; }
        JsonObject json() { return JsonParser.parseString(response.body()).getAsJsonObject(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        try {
            run();
            System.out.println("StorageVerificationTest: 31 checks passed");
        } catch (Throwable failure) {
            System.out.println("FAIL: " + failure);
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        try (RunningApp running = new RunningApp()) {
            HttpResponse<String> anonymous = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(running.base.resolve("/api/admin/storage")).GET().build(), HttpResponse.BodyHandlers.ofString());
            check(anonymous.statusCode() == 401, "storage status rejects anonymous users");

            HttpResponse<String> statusResponse = running.get("/api/admin/storage");
            check(statusResponse.statusCode() == 200, "admin can read storage status");
            JsonObject status = JsonParser.parseString(statusResponse.body()).getAsJsonObject();
            check("file".equals(status.get("backend").getAsString()), "status reports actual store backend");
            check(status.get("durable").getAsBoolean(), "status reports durable store configured");
            check(status.get("writeReady").getAsBoolean(), "initially writable store is ready");
            check(status.has("employeeRecords") && status.has("attendanceRecords"), "status reports counts without exposing rows");

            HttpResponse<String> noCsrf = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"VERIFY\"}", false);
            check(noCsrf.statusCode() == 403, "verification requires CSRF");
            HttpResponse<String> wrongConfirmation = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"yes\"}", true);
            check(wrongConfirmation.statusCode() == 400, "verification requires the explicit VERIFY confirmation");
            check(running.get("/api/admin/storage").statusCode() == 200, "bad confirmation does not pause writes");

            int savesBefore = running.store.saveCalls;
            HttpResponse<String> verified = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"VERIFY\"}", true);
            check(verified.statusCode() == 200, "round trip succeeds against the configured store");
            JsonObject roundTrip = JsonParser.parseString(verified.body()).getAsJsonObject();
            check(roundTrip.get("readBeforeWrite").getAsBoolean(), "verifier read the store first");
            check(roundTrip.get("stateMatchedBeforeWrite").getAsBoolean(), "saved snapshot matched running state");
            check(roundTrip.get("writeCommitted").getAsBoolean(), "unchanged snapshot write committed");
            check(roundTrip.get("readBackMatched").getAsBoolean(), "immediate read-back matched");
            check(running.store.saveCalls == savesBefore + 2, "round trip writes once, then normal request auditing is persisted");
            JsonObject afterSuccess = JsonParser.parseString(running.get("/api/admin/storage").body()).getAsJsonObject();
            check(afterSuccess.getAsJsonObject("lastVerification").get("readBackMatched").getAsBoolean(), "successful check is retained for this process");

            // Missing snapshot: do not write a replacement, pause mutations, and retain an in-memory audit event.
            running.store.state = null;
            int savesBeforeMissing = running.store.saveCalls;
            HttpResponse<String> missing = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"VERIFY\"}", true);
            check(missing.statusCode() == 409, "missing snapshot is refused");
            check(running.store.saveCalls == savesBeforeMissing, "missing snapshot is not replaced");
            String pausedAfterMissing = running.get("/api/admin/storage").body();
            check(pausedAfterMissing.contains("\"writeReady\":false"), "missing snapshot pauses writes: " + pausedAfterMissing);
            check(running.get("/api/admin/audit?path=storage%2Fverify&failures=true").body().contains("\"lg_iStatus\":409"), "failed check is visible in current-process audit");

            running.store.state = running.app.exportState();
            running.resetBrokenFlag();
            running.store.failNextLoad = true;
            int savesBeforeReadFailure = running.store.saveCalls;
            HttpResponse<String> readFailure = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"VERIFY\"}", true);
            check(readFailure.statusCode() == 503, "store read failure returns unavailable");
            check(running.store.saveCalls == savesBeforeReadFailure, "read failure does not write the store");
            check(running.get("/api/admin/storage").body().contains("\"writeReady\":false"), "read failure pauses writes");

            running.store.state = running.app.exportState();
            running.resetBrokenFlag();
            running.store.failNextSave = true;
            HttpResponse<String> writeFailure = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"VERIFY\"}", true);
            check(writeFailure.statusCode() == 503, "store write failure returns unavailable");
            check(running.get("/api/admin/storage").body().contains("\"writeReady\":false"), "write failure pauses writes");

            running.store.state = running.app.exportState();
            running.resetBrokenFlag();
            running.store.failReadBackAfterSave = true;
            HttpResponse<String> readBackFailure = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"VERIFY\"}", true);
            check(readBackFailure.statusCode() == 503, "read-back failure returns unavailable");
            check(running.get("/api/admin/storage").body().contains("\"writeReady\":false"), "read-back failure pauses writes");

            running.store.state = running.app.exportState();
            running.resetBrokenFlag();
            JsonObject mismatch = running.store.state.deepCopy();
            mismatch.getAsJsonObject("employees").addProperty("testOnlyTamper", true);
            running.store.state = mismatch.deepCopy();
            int savesBeforeMismatch = running.store.saveCalls;
            HttpResponse<String> mismatched = running.rawPost("/api/admin/storage/verify", "{\"confirm\":\"VERIFY\"}", true);
            check(mismatched.statusCode() == 409, "pre-write mismatch is refused");
            check(running.store.saveCalls == savesBeforeMismatch, "pre-write mismatch is not overwritten");
            check(running.store.load().get().equals(mismatch), "mismatched snapshot remains unchanged");
            check(running.get("/api/admin/storage").body().contains("\"writeReady\":false"), "mismatch pauses writes");
        }
    }
}
