package ph.motorph.t_tkt;

import ph.motorph.o_oop.FirestoreRestClient;
import ph.motorph.t_tkt.LeaveRequest;
import ph.motorph.t_tkt.LeaveTicket;
import ph.motorph.t_tkt.FirestoreWorkflowStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class FirestoreWorkflowStoreTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        createsRequestsInTheCurrentUserSubcollection();
        listsTicketsAndMapsFirestoreFields();
        cancelsOnlyByPatchingStatusAndTimestamp();
        System.out.println("PASS: " + checks + " Firestore workflow-store checks");
    }

    private static void createsRequestsInTheCurrentUserSubcollection() throws Exception {
        String id = "LV-012345ABCDEF";
        try (FakeFirestore server = new FakeFirestore(new Reply(200,
            "{\"name\":\"projects/demo/databases/(default)/documents/users/user-7/demoLeaveRequests/" + id + "\"}"))) {
            FirestoreWorkflowStore store = new FirestoreWorkflowStore(client(server));
            LeaveRequest request = new LeaveRequest(id, "DEMO-001", "Mara Rivera", "Vacation",
                "2026-11-10", "2026-11-11", "Imported only", "Pending review", "2026-11-03T10:00:00Z", "");
            check(store.createLeaveRequest(request) == request, "created workflow returned to caller");
            check(server.methods.get(0).equals("POST"), "workflow creation uses Firestore POST");
            check(server.paths.get(0).contains("/users/user-7/demoLeaveRequests")
                && server.paths.get(0).contains("documentId=" + id), "leave request path is scoped to user UID");
            check(server.bodies.get(0).contains("Pending review") && server.bodies.get(0).contains("timestampValue"),
                "leave fields use Firestore string and timestamp types");
            check("Bearer test-id-token".equals(server.authorization.get()), "ID token is forwarded for Firestore authorization");
        }
    }

    private static void listsTicketsAndMapsFirestoreFields() throws Exception {
        String document = "{\"name\":\"projects/demo/databases/(default)/documents/users/user-7/demoTickets/TK-012345ABCDEF\","
            + "\"fields\":{\"employeeId\":{\"stringValue\":\"DEMO-002\"},\"employeeName\":{\"stringValue\":\"Eli Santos\"},"
            + "\"ticketType\":{\"stringValue\":\"Timesheet correction\"},\"queue\":{\"stringValue\":\"Team Lead\"},"
            + "\"summary\":{\"stringValue\":\"Imported correction\"},\"status\":{\"stringValue\":\"Open\"},"
            + "\"submittedAt\":{\"timestampValue\":\"2026-11-03T10:00:00Z\"}}}";
        try (FakeFirestore server = new FakeFirestore(new Reply(200, "{\"documents\":[" + document + "]}"))) {
            List<LeaveTicket> tickets = new FirestoreWorkflowStore(client(server)).listTickets();
            check(tickets.size() == 1, "one ticket mapped from user collection");
            check("TK-012345ABCDEF".equals(tickets.get(0).getId()), "ticket ID mapped from document path");
            check("Team Lead".equals(tickets.get(0).getQueue()), "ticket queue label mapped");
            check("Open".equals(tickets.get(0).getStatus()), "ticket status mapped");
            check(server.paths.get(0).contains("/users/user-7/demoTickets"), "ticket list uses user-scoped collection");
        }
    }

    private static void cancelsOnlyByPatchingStatusAndTimestamp() throws Exception {
        String id = "LV-012345ABCDEF";
        String document = "{\"name\":\"projects/demo/databases/(default)/documents/users/user-7/demoLeaveRequests/" + id + "\","
            + "\"fields\":{\"employeeId\":{\"stringValue\":\"DEMO-001\"},\"employeeName\":{\"stringValue\":\"Mara Rivera\"},"
            + "\"leaveType\":{\"stringValue\":\"Vacation\"},\"startDate\":{\"stringValue\":\"2026-11-10\"},"
            + "\"endDate\":{\"stringValue\":\"2026-11-11\"},\"note\":{\"stringValue\":\"\"},"
            + "\"status\":{\"stringValue\":\"Pending review\"},\"submittedAt\":{\"timestampValue\":\"2026-11-03T10:00:00Z\"}}}";
        try (FakeFirestore server = new FakeFirestore(
            new Reply(200, "{\"documents\":[" + document + "]}"),
            new Reply(200, "{\"name\":\"projects/demo/databases/(default)/documents/users/user-7/demoLeaveRequests/" + id + "\"}"))) {
            LeaveRequest cancelled = new FirestoreWorkflowStore(client(server)).cancelLeaveRequest(id, "2026-11-03T11:00:00Z");
            check("Cancelled".equals(cancelled.getStatus()), "cancelled response reflects new state");
            check(server.methods.equals(Arrays.asList("GET", "PATCH")), "cancellation reads current state then patches");
            check(server.paths.get(1).contains("updateMask.fieldPaths=status")
                && server.paths.get(1).contains("updateMask.fieldPaths=cancelledAt"), "patch mask contains only cancellation fields");
            check(server.bodies.get(1).contains("Cancelled") && !server.bodies.get(1).contains("employeeName"),
                "cancellation cannot rewrite employee or request fields");
        }
    }

    private static FirestoreRestClient client(FakeFirestore server) {
        return new FirestoreRestClient("demo", "user-7", "test-id-token", server.apiRoot());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Reply {
        private final int status;
        private final String body;
        Reply(int status, String body) { this.status = status; this.body = body; }
    }

    private static final class FakeFirestore implements AutoCloseable {
        private final HttpServer server;
        private final List<Reply> replies;
        private final List<String> methods = new ArrayList<>();
        private final List<String> paths = new ArrayList<>();
        private final List<String> bodies = new ArrayList<>();
        private final AtomicReference<String> authorization = new AtomicReference<>();
        private int nextReply;

        FakeFirestore(Reply... replies) throws IOException {
            this.replies = Arrays.asList(replies);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        String apiRoot() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/projects/"; }

        private synchronized void handle(HttpExchange exchange) throws IOException {
            methods.add(exchange.getRequestMethod());
            paths.add(exchange.getRequestURI().toString());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            Reply reply = replies.get(Math.min(nextReply++, replies.size() - 1));
            byte[] bytes = reply.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override public void close() { server.stop(0); }
    }
}
