package ph.motorph.svc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.Optional;
import ph.motorph.o_oop.LocalStateFile;
import ph.motorph.o_oop.StateStore;

public final class WriteAcknowledgementTest {
    private static final class FailedStore implements StateStore {
        final Gson gson = new LocalStateFile(Paths.get("unused.json")).gson();
        public Optional<JsonObject> load() { return Optional.empty(); }
        public void save(JsonObject data) throws IOException { throw new IOException("controlled failure"); }
        public Gson gson() { return gson; }
        public String describe() { return "controlled test store"; }
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        AppServer app = new AppServer("", Paths.get("web"), null, null);
        Field field = AppServer.class.getDeclaredField("dataFile");
        field.setAccessible(true);
        field.set(app, new FailedStore());
        int port = app.start("127.0.0.1", 0).getAddress().getPort();
        try {
            HttpClient client = HttpClient.newHttpClient();
            URI uri = URI.create("http://127.0.0.1:" + port + "/api/auth/signin");
            HttpRequest request = HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"employeeId\":\"10006\",\"password\":\"MotorPH-demo-2026\"}"))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 503, "failed save must not return a success");
            check(!response.body().contains("challengeId"), "failed save must not expose a successful response");
            check(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 503,
                "new writes must be refused after uncertain persistence");
        } finally { app.stop(); }
        System.out.println("WriteAcknowledgementTest: 3 checks passed");
    }
}
