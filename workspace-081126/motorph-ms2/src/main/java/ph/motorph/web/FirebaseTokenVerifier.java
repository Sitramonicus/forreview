package ph.motorph.web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Verifies a Firebase ID token, and answers "which account is this?".
 *
 * <p>Before this existed the server treated <em>any</em> {@code Authorization} header as proof of
 * sign-in. That is not authentication: it meant anyone who could send a header could name any employee
 * number and be given that person's role. Every claim about "the browser cannot promote itself" was
 * therefore false in production.
 *
 * <p>Verification is a call to Google's {@code accounts:lookup} endpoint with the project's <b>Web API
 * key</b>. That endpoint validates the token's signature, expiry and issuer, and — decisively for this
 * design — returns the account's <b>uid</b> and <b>email</b>. The answer is cached briefly so a page
 * load does not make ten identical calls.
 *
 * <p>Note what this deliberately does <em>not</em> do: it never reads the employee number out of the
 * token. There is no such field to read, and if there were, the client could forge it. The employee
 * number is looked up from the verified uid in the seat registry.
 */
public final class FirebaseTokenVerifier {

    /** The verified account behind a token. */
    public static final class Account {
        private final String uid;
        private final String email;
        private final long expiresAtMillis;

        Account(String uid, String email, long expiresAtMillis) {
            this.uid = uid;
            this.email = email;
            this.expiresAtMillis = expiresAtMillis;
        }

        public String uid() {
            return uid;
        }

        public String email() {
            return email;
        }

        long expiresAtMillis() {
            return expiresAtMillis;
        }
    }

    /** Cache entry: the account, and when we should stop trusting our copy. */
    private static final class Cached {
        final Account account;
        final Instant refreshAfter;

        Cached(Account account, Instant refreshAfter) {
            this.account = account;
            this.refreshAfter = refreshAfter;
        }
    }

    private final String projectId;
    private final String apiKey;
    private final URI lookupEndpoint;
    private final Duration cacheTtl;
    private final HttpClient http;
    private final java.util.Map<String, Cached> cache =
        new java.util.concurrent.ConcurrentHashMap<>();

    public FirebaseTokenVerifier(String projectId, String apiKey, String apiRoot, Duration cacheTtl) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("A Firebase project id is required to verify tokens.");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("A Firebase Web API key is required to verify tokens. "
                + "Set FIREBASE_API_KEY (or VITE_FIREBASE_API_KEY for the browser).");
        }
        this.projectId = projectId.trim();
        this.apiKey = apiKey.trim();
        this.lookupEndpoint = URI.create(
            (apiRoot == null || apiRoot.trim().isEmpty()
                ? "https://identitytoolkit.googleapis.com/v1"
                : apiRoot.trim())
                + "/accounts:lookup?key=" + this.apiKey);
        this.cacheTtl = cacheTtl == null ? Duration.ofMinutes(2) : cacheTtl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * @throws AccessControl.Denied 401 when the token is missing, expired, for another project, or not
     *                              a valid Firebase ID token at all
     */
    public Account verify(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            throw new AccessControl.Denied("Sign in with your MotorPH account to continue.", 401);
        }
        String token = idToken.trim();
        Cached hit = cache.get(token);
        if (hit != null && Instant.now().isBefore(hit.refreshAfter)) return hit.account;

        String body;
        try {
            HttpRequest request = HttpRequest.newBuilder(lookupEndpoint)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"idToken\":" + quote(token) + "}", StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            body = response.body();
            if (response.statusCode() != 200) {
                throw new AccessControl.Denied(describeRefusal(body), 401);
            }
        } catch (IOException ex) {
            // Fail closed. A verifying outage must never become an authentication bypass.
            throw new AccessControl.Denied("Could not verify your session with Firebase right now. "
                + "Try again in a moment.", 503);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AccessControl.Denied("Could not verify your session with Firebase.", 503);
        }

        Account account;
        try {
            account = parse(body);
        } catch (RuntimeException ex) {
            throw new AccessControl.Denied("Firebase returned an unreadable session response.", 401);
        }
        cache.put(token, new Cached(account, Instant.now().plus(cacheTtl)));
        return account;
    }

    private Account parse(String body) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException ex) {
            throw new AccessControl.Denied("Firebase returned an unreadable session response.", 401);
        }
        if (!root.has("users") || !root.getAsJsonArray("users").isJsonArray()
            || root.getAsJsonArray("users").size() == 0) {
            throw new AccessControl.Denied("That session is no longer valid. Sign in again.", 401);
        }
        JsonObject user = root.getAsJsonArray("users").get(0).getAsJsonObject();
        String uid = user.has("localId") ? user.get("localId").getAsString() : "";
        if (uid.isEmpty()) {
            throw new AccessControl.Denied("That session is no longer valid. Sign in again.", 401);
        }
        String email = user.has("email") ? user.get("email").getAsString() : "";
        // The project check: a token minted by a different Firebase project must not be accepted.
        if (user.has("tenantId") && !user.get("tenantId").isJsonNull()
            && !projectId.equals(user.get("tenantId").getAsString())) {
            throw new AccessControl.Denied("That session belongs to another Firebase project.", 401);
        }
        long expiresAt = 0L;
        if (user.has("validUntil")) {
            // validUntil is seconds since epoch; it is the account's session expiry, not the token's,
            // so it is used only to shorten the cache, never to decide validity.
            expiresAt = user.get("validUntil").getAsLong() * 1000L;
        }
        return new Account(uid, email, expiresAt);
    }

    /** Turns Firebase's terse error codes into something a person can act on. */
    private static String describeRefusal(String body) {
        String code = "";
        try {
            JsonObject error = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error");
            if (error != null && error.has("message")) code = error.get("message").getAsString();
        } catch (RuntimeException ignored) {
            // Fall through to the generic message.
        }
        if (code.contains("API_KEY_INVALID") || code.contains("API key not valid")) {
            return "This server's Firebase API key is invalid, so sign-ins cannot be verified. "
                + "Check FIREBASE_API_KEY.";
        }
        if (code.contains("INVALID_ID_TOKEN") || code.contains("TOKEN_EXPIRED")
            || code.contains("INVALID_ARGUMENT")) {
            return "Your session has expired. Sign in again.";
        }
        if (code.contains("USER_DISABLED")) {
            return "This account has been disabled. Ask the payroll office.";
        }
        return "Sign in with your MotorPH account to continue.";
    }

    private static String abbreviate(String value) {
        if (value == null) return "";
        return value.length() <= 200 ? value : value.substring(0, 200) + "...";
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }

    /** Drops expired cache entries. Called opportunistically, never required. */
    public void pruneCache() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> {
            Cached value = entry.getValue();
            return now.isAfter(value.refreshAfter)
                || (value.account.expiresAtMillis() > 0
                    && now.toEpochMilli() > value.account.expiresAtMillis());
        });
    }

    public int cacheSize() {
        return cache.size();
    }
}
