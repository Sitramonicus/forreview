package ph.motorph.o_oop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class FirestoreRestClient {
    private static final String API_ROOT = "https://firestore.googleapis.com/v1/projects/";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();

    private final String apiRoot;
    private final String projectId;
    private final String userId;
    public interface TokenSource {
        String token() throws IOException;
        default boolean refresh() throws IOException { return false; }
    }
    private final TokenSource tokenSource;

    public FirestoreRestClient(String projectId, String userId, String idToken) {
        this(projectId, userId, idToken, API_ROOT);
    }

    
    public FirestoreRestClient(String projectId, String userId, String idToken, String apiRoot) {
        this(projectId, userId, fixedToken(idToken), apiRoot);
    }

    private static TokenSource fixedToken(String token) {
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("A Firestore token is required");
        String value = token.trim();
        return () -> value;
    }

    public FirestoreRestClient(String projectId, String userId, TokenSource tokenSource, String apiRoot) {
        this.projectId = requireSafeSegment(projectId, "project id");
        this.userId = requireSafeSegment(userId, "user id");
        if (tokenSource == null) throw new IllegalArgumentException("Firestore credentials are required");
        if (apiRoot == null || apiRoot.trim().isEmpty()) throw new IllegalArgumentException("API root must not be blank");
        this.tokenSource = tokenSource;
        this.apiRoot = apiRoot.trim().endsWith("/") ? apiRoot.trim() : apiRoot.trim() + "/";
    }

    public static FirestoreRestClient applicationDefault(String projectId, String userId, String apiRoot) throws IOException {
        return new FirestoreRestClient(projectId, userId, new GoogleAccessToken(), apiRoot == null ? API_ROOT : apiRoot);
    }

    public List<JsonObject> listDocuments(String collectionPath) {
        List<JsonObject> documents = new ArrayList<>();
        String pageToken = "";
        do {
            String url = documentsUrl(collectionPath) + "?pageSize=1000" + pageToken;
            JsonObject response = send("GET", url, null, false);
            JsonArray items = response.has("documents") ? response.getAsJsonArray("documents") : new JsonArray();
            for (JsonElement item : items) documents.add(item.getAsJsonObject());
            pageToken = response.has("nextPageToken")
                ? "&pageToken=" + queryEncode(response.get("nextPageToken").getAsString()) : "";
        } while (!pageToken.isEmpty());
        return documents;
    }

    public boolean createDocument(String collectionPath, String documentId, JsonObject fields) {
        String url = documentsUrl(collectionPath) + "?documentId=" + queryEncode(requireSafeSegment(documentId, "document id"));
        JsonObject body = new JsonObject();
        body.add("fields", fields);
        return send("POST", url, body, true).has("name");
    }

    /** Reads one document by id, or null when it does not exist. Never lists the collection. */
    public JsonObject getDocument(String collectionPath, String documentId) {
        String url = documentsUrl(collectionPath) + "/" + pathEncode(requireSafeSegment(documentId, "document id"));
        try {
            return send("GET", url, null, false);
        } catch (FirestoreException ex) {
            if (ex.getStatusCode() == 404) return null;
            throw ex;
        }
    }

    /** Writes one document, creating it or overwriting the given fields. */
    public boolean setDocument(String collectionPath, String documentId, JsonObject fields) {
        if (fields == null || fields.size() == 0) throw new IllegalArgumentException("document fields must not be empty");
        return updateDocument(collectionPath, documentId, fields, new ArrayList<>(fields.keySet()));
    }

    public boolean updateDocument(String collectionPath, String documentId, JsonObject fields, List<String> updateMask) {
        if (fields == null) throw new IllegalArgumentException("document fields must not be null");
        if (updateMask == null || updateMask.isEmpty()) throw new IllegalArgumentException("update mask must not be empty");
        StringBuilder url = new StringBuilder(documentsUrl(collectionPath))
            .append('/').append(pathEncode(requireSafeSegment(documentId, "document id")));
        for (String field : updateMask) {
            String safeField = requireSafeSegment(field, "update field");
            url.append(url.indexOf("?") < 0 ? "?" : "&")
                .append("updateMask.fieldPaths=").append(queryEncode(safeField));
        }
        JsonObject body = new JsonObject();
        body.add("fields", fields);
        return send("PATCH", url.toString(), body, false).has("name");
    }

    /** Deletes one document. Missing documents count as already deleted. */
    public boolean deleteDocument(String collectionPath, String documentId) {
        String url = documentsUrl(collectionPath) + "/" + pathEncode(requireSafeSegment(documentId, "document id"));
        try {
            send("DELETE", url, null, false);
            return true;
        } catch (FirestoreException ex) {
            if (ex.getStatusCode() == 404) return true;
            throw ex;
        }
    }

    public JsonObject commitDocuments(java.util.Map<String, JsonObject> updates, java.util.List<String> deletes,
            String guardedPath, String expectedUpdateTime, boolean expectedExists) {
        JsonArray writes = new JsonArray();
        String root = "projects/" + projectId + "/databases/(default)/documents/";
        for (java.util.Map.Entry<String, JsonObject> entry : updates.entrySet()) {
            JsonObject document = new JsonObject();
            document.addProperty("name", root + entry.getKey());
            document.add("fields", entry.getValue());
            JsonObject write = new JsonObject();
            write.add("update", document);
            if (entry.getKey().equals(guardedPath)) {
                JsonObject condition = new JsonObject();
                if (expectedUpdateTime != null && !expectedUpdateTime.isEmpty()) condition.addProperty("updateTime", expectedUpdateTime);
                else condition.addProperty("exists", expectedExists);
                write.add("currentDocument", condition);
            }
            writes.add(write);
        }
        for (String name : deletes) {
            JsonObject write = new JsonObject();
            write.addProperty("delete", root + name);
            writes.add(write);
        }
        JsonObject body = new JsonObject();
        body.add("writes", writes);
        if (body.toString().getBytes(StandardCharsets.UTF_8).length > 9_500_000)
            throw new FirestoreException("The state is too large for one atomic Firestore commit.", 413);
        return send("POST", apiRoot + pathEncode(projectId) + "/databases/(default)/documents:commit", body, false);
    }

    public String userCollection(String collectionName) {
        return "users/" + userId + "/" + requireSafeSegment(collectionName, "collection name");
    }

    private JsonObject send(String method, String url, JsonObject body, boolean allowExisting) {
        return send(method, url, body, allowExisting, false);
    }

    private JsonObject send(String method, String url, JsonObject body, boolean allowExisting, boolean retried) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
            String token = tokenSource.token();
            if (token != null && !token.isEmpty()) builder.header("Authorization", "Bearer " + token);
            if (body != null) {
                builder.header("Content-Type", "application/json; charset=utf-8")
                    .method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401 && !retried && tokenSource.refresh())
                return send(method, url, body, allowExisting, true);
            if (allowExisting && (response.statusCode() == 409 || response.statusCode() == 400)
                && response.body() != null && response.body().contains("ALREADY_EXISTS")) {
                return new JsonObject();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw firestoreError(response.statusCode(), response.body());
            }
            String responseBody = response.body();
            return responseBody == null || responseBody.trim().isEmpty()
                ? new JsonObject() : JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Firestore request was interrupted.", 503);
        } catch (IOException | IllegalArgumentException ex) {
            throw new FirestoreException("Could not reach Firestore. Check the network and project setup.", 503);
        }
    }

    private String documentsUrl(String collectionPath) {
        StringBuilder path = new StringBuilder(apiRoot)
            .append(pathEncode(projectId)).append("/databases/(default)/documents");
        for (String segment : collectionPath.split("/")) {
            path.append('/').append(pathEncode(requireSafeSegment(segment, "path segment")));
        }
        return path.toString();
    }

    private static String requireSafeSegment(String value, String label) {
        if (value == null || value.trim().isEmpty() || value.contains("/")) {
            throw new IllegalArgumentException(label + " must be a non-empty path segment");
        }
        return value.trim();
    }

    private static String pathEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String queryEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static FirestoreException firestoreError(int status, String body) {
        String message = "Firestore request failed (HTTP " + status + ").";
        if (status == 401) message = "Google credentials were rejected. Check the server identity and Firestore permissions.";
        else if (status == 403) message = "Firestore denied this request. Publish the scoped development rules in docs/firestore.rules, then retry.";
        else if (status == 404) message = "Firestore database or requested resource was not found. Check the Firebase project and database setup.";
        else if (status == 409 || status == 400) message = "A demo record could not be created. Refresh the page and try again.";
        return new FirestoreException(message, status);
    }
}
