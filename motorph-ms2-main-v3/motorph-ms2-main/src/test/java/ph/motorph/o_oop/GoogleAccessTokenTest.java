package ph.motorph.o_oop;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public final class GoogleAccessTokenTest {
    private static final class RotatingCredentials extends GoogleCredentials {
        int refreshes;
        @Override public AccessToken refreshAccessToken() {
            refreshes++;
            return new AccessToken("token-" + refreshes, new Date(System.currentTimeMillis() + 3600000));
        }
    }
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
    public static void main(String[] args) throws Exception {
        RotatingCredentials credentials = new RotatingCredentials();
        GoogleAccessToken source = new GoogleAccessToken(credentials);
        check(source.token().equals("token-1"), "first request obtains a token");
        check(source.token().equals("token-1") && credentials.refreshes == 1, "unexpired token is reused");
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            int call = calls.incrementAndGet();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            boolean accepted = call > 1 && "Bearer token-2".equals(auth);
            byte[] response = (accepted ? "{\"documents\":[]}" : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(accepted ? 200 : 401, response.length);
            try (java.io.OutputStream out = exchange.getResponseBody()) { out.write(response); }
        });
        server.start();
        try {
            FirestoreRestClient client = new FirestoreRestClient("test-project", "server", source,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/projects/");
            check(client.listDocuments("records").isEmpty(), "request succeeds after token refresh");
            check(calls.get() == 2 && credentials.refreshes == 2, "401 refreshes and retries once");
        } finally { server.stop(0); }
        System.out.println("GoogleAccessTokenTest: 4 checks passed");
    }
}
