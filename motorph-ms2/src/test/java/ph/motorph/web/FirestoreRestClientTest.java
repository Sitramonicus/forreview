package ph.motorph.web;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Offline tests for Firestore REST token forwarding, document mapping, and errors. */
public final class FirestoreRestClientTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        listsUserDocumentsWithTheBearerTokenAndMapsEmployeeFields();
        mapsPermissionDenialWithoutExposingRequestCredentials();
        createsDocumentsWithTypedFieldsAndHandlesAlreadyExistingSeedRecords();
        updatesOnlyTheRequestedFieldsForWorkflowCancellation();
        rejectsPathInjectionInUserIdentifiers();
        System.out.println("PASS: " + checks + " Firestore REST adapter checks");
    }

    private static void listsUserDocumentsWithTheBearerTokenAndMapsEmployeeFields() throws Exception {
        String response = "{\"documents\":[{\"name\":\"projects/motorph-jd/databases/(default)/documents/users/user-123/demoEmployees/DEMO-001\","
            + "\"fields\":{\"id\":{\"stringValue\":\"DEMO-001\"},\"name\":{\"stringValue\":\"Mara Rivera\"},"
            + "\"department\":{\"stringValue\":\"People Operations\"},\"role\":{\"stringValue\":\"HR Coordinator\"},"
            + "\"location\":{\"stringValue\":\"Makati\"},\"status\":{\"stringValue\":\"Active\"}}}]}";
        try (FakeFirestoreServer server = new FakeFirestoreServer(200, response)) {
            FirestoreRestClient client = client(server);
            List<Employee> employees = new FirestoreEmployeeStore(client).findAll();
            check(employees.size() == 1, "one employee returned");
            check("Mara Rivera".equals(employees.get(0).getName()), "employee name mapped");
            check("Active".equals(employees.get(0).getStatus()), "employee status mapped");
            check("Bearer test-id-token".equals(server.authorization.get()), "Firebase ID token forwarded as bearer");
            check("GET".equals(server.method.get()), "collection read uses GET");
            check(server.path.get().contains("/users/user-123/demoEmployees"), "read is scoped to the supplied user path");
        }
    }

    private static void mapsPermissionDenialWithoutExposingRequestCredentials() throws Exception {
        try (FakeFirestoreServer server = new FakeFirestoreServer(403,
            "{\"error\":{\"code\":403,\"status\":\"PERMISSION_DENIED\"}}")) {
            try {
                client(server).listDocuments("users/user-123/demoEmployees");
                throw new AssertionError("expected FirestoreException for permission denial");
            } catch (FirestoreException ex) {
                check(ex.getStatusCode() == 403, "permission denial keeps HTTP 403");
                check(ex.getMessage().contains("Firestore denied"), "permission denial has actionable text");
                check(!ex.getMessage().contains("test-id-token"), "error does not include ID token");
            }
        }
    }

    private static void createsDocumentsWithTypedFieldsAndHandlesAlreadyExistingSeedRecords() throws Exception {
        JsonObject fields = new JsonObject();
        JsonObject id = new JsonObject();
        id.addProperty("stringValue", "DEMO-001");
        fields.add("id", id);
        try (FakeFirestoreServer server = new FakeFirestoreServer(200,
            "{\"name\":\"projects/motorph-jd/databases/(default)/documents/users/user-123/demoEmployees/DEMO-001\"}")) {
            FirestoreRestClient client = client(server);
            check(client.createDocument(client.userCollection("demoEmployees"), "DEMO-001", fields), "new document reports created");
            check("POST".equals(server.method.get()), "document creation uses POST");
            check(server.path.get().contains("documentId=DEMO-001"), "document ID is explicit");
            check(server.requestBody.get().contains("stringValue"), "Firestore typed field body is sent");
        }
        try (FakeFirestoreServer server = new FakeFirestoreServer(409,
            "{\"error\":{\"code\":409,\"status\":\"ALREADY_EXISTS\"}}")) {
            check(!client(server).createDocument("users/user-123/demoEmployees", "DEMO-001", fields),
                "re-seeding does not overwrite an existing document");
        }
    }

    private static void updatesOnlyTheRequestedFieldsForWorkflowCancellation() throws Exception {
        JsonObject fields = new JsonObject();
        JsonObject status = new JsonObject();
        status.addProperty("stringValue", "Cancelled");
        JsonObject cancelledAt = new JsonObject();
        cancelledAt.addProperty("timestampValue", "2026-11-03T10:15:30Z");
        fields.add("status", status);
        fields.add("cancelledAt", cancelledAt);
        try (FakeFirestoreServer server = new FakeFirestoreServer(200,
            "{\"name\":\"projects/motorph-jd/databases/(default)/documents/users/user-123/demoLeaveRequests/LV-012345ABCDEF\"}")) {
            FirestoreRestClient client = client(server);
            check(client.updateDocument("users/user-123/demoLeaveRequests", "LV-012345ABCDEF", fields,
                java.util.Arrays.asList("status", "cancelledAt")), "workflow cancellation patch succeeds");
            check("PATCH".equals(server.method.get()), "workflow status uses PATCH, not delete or recreate");
            check(server.path.get().contains("updateMask.fieldPaths=status")
                && server.path.get().contains("updateMask.fieldPaths=cancelledAt"), "patch mask limits changed Firestore fields");
            check(server.requestBody.get().contains("Cancelled") && server.requestBody.get().contains("timestampValue"),
                "cancellation sends only new status and timestamp values");
        }
    }

    private static void rejectsPathInjectionInUserIdentifiers() {
        try {
            new FirestoreRestClient("motorph-jd", "user/other", "token");
            throw new AssertionError("expected invalid UID to be rejected");
        } catch (IllegalArgumentException expected) {
            check(true, "UID cannot escape its Firestore path segment");
        }
    }

    private static FirestoreRestClient client(FakeFirestoreServer server) {
        return new FirestoreRestClient("motorph-jd", "user-123", "test-id-token", server.apiRoot());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class FakeFirestoreServer implements AutoCloseable {
        private final HttpServer server;
        private final int status;
        private final String responseBody;
        private final AtomicReference<String> authorization = new AtomicReference<>();
        private final AtomicReference<String> method = new AtomicReference<>();
        private final AtomicReference<String> path = new AtomicReference<>();
        private final AtomicReference<String> requestBody = new AtomicReference<>("");

        FakeFirestoreServer(int status, String responseBody) throws IOException {
            this.status = status;
            this.responseBody = responseBody;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        String apiRoot() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/projects/";
        }

        private void handle(HttpExchange exchange) throws IOException {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().toString());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
