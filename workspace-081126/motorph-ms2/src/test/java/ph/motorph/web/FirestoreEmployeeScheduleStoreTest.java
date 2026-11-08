package ph.motorph.web;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Offline checks that schedule attribution comes only from the caller's Firestore data. */
public final class FirestoreEmployeeScheduleStoreTest {
    private int checks;

    public static void main(String[] args) throws Exception {
        new FirestoreEmployeeScheduleStoreTest().run();
    }

    private void run() throws Exception {
        List<String> paths = new ArrayList<>();
        List<String> authHeaders = new ArrayList<>();
        HttpServer fake = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fake.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().toString());
            authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = "{\"documents\":["
                + "{\"name\":\"projects/demo/databases/(default)/documents/users/user-77/demoEmployeeSchedules/10001\","
                + "\"fields\":{\"employeeId\":{\"stringValue\":\"10001\"},\"scheduleId\":{\"integerValue\":\"101\"}}},"
                + "{\"name\":\"projects/demo/databases/(default)/documents/users/user-77/demoEmployeeSchedules/BAD-NUMBER\","
                + "\"fields\":{\"employeeId\":{\"stringValue\":\"BAD-NUMBER\"},\"scheduleId\":{\"integerValue\":\"oops\"}}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        fake.start();
        try {
            String apiRoot = "http://127.0.0.1:" + fake.getAddress().getPort() + "/v1/projects/";
            FirestoreRestClient client = new FirestoreRestClient("demo-project", "user-77", "test-id-token", apiRoot);
            FirestoreEmployeeScheduleStore store = new FirestoreEmployeeScheduleStore(client);
            OptionalInt mapped = store.findScheduleIdForEmployee("10001");
            check(mapped.isPresent() && mapped.getAsInt() == 101, "reads existing mapping from the Firestore document");
            check(!store.findScheduleIdForEmployee("10002").isPresent(), "does not fall back to an in-code fixture mapping");
            check(!store.findScheduleIdForEmployee("BAD-NUMBER").isPresent(), "ignores malformed schedule IDs");
            check(!store.findScheduleIdForEmployee(" ").isPresent(), "blank employee IDs are not looked up");
            check(paths.size() == 1, "mapping collection is loaded once per request-scoped store");
            check(paths.get(0).contains("/users/user-77/demoEmployeeSchedules"), "reads only the caller UID mapping path");
            check("Bearer test-id-token".equals(authHeaders.get(0)), "forwards the Firebase ID token to Firestore");
        } finally {
            fake.stop(0);
        }
        System.out.println("PASS: " + checks + " Firestore employee-schedule mapping checks");
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
