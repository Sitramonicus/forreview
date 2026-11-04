package ph.motorph.web;

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

/**
 * Small Firestore REST client that forwards a Firebase Auth ID token. Firestore
 * Security Rules, not server credentials, authorize every read and write.
 */
public final class FirestoreRestClient {
    private static final String API_ROOT = "https://firestore.googleapis.com/v1/projects/";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();

    private final String apiRoot;
    private final String projectId;
    private final String userId;
    private final String idToken;

    public FirestoreRestClient(String projectId, String userId, String idToken) {
        this(projectId, userId, idToken, API_ROOT);
    }

    /** Package-private endpoint override for isolated HTTP contract tests. */
    FirestoreRestClient(String projectId, String userId, String idToken, String apiRoot) {
        this.projectId = requireSafeSegment(projectId, "project id");
        this.userId = requireSafeSegment(userId, "user id");
        if (idToken == null || idToken.trim().isEmpty()) throw new IllegalArgumentException("Firebase sign-in is required");
        if (apiRoot == null || apiRoot.trim().isEmpty()) throw new IllegalArgumentException("API root must not be blank");
        this.idToken = idToken.trim();
        this.apiRoot = apiRoot.trim().endsWith("/") ? apiRoot.trim() : apiRoot.trim() + "/";
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

    public String userCollection(String collectionName) {
        return "users/" + userId + "/" + requireSafeSegment(collectionName, "collection name");
    }

    private JsonObject send(String method, String url, JsonObject body, boolean allowExisting) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + idToken)
                .header("Accept", "application/json");
            if (body != null) {
                builder.header("Content-Type", "application/json; charset=utf-8")
                    .method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
        if (status == 401) message = "Firebase sign-in expired. Sign out and sign in again.";
        else if (status == 403) message = "Firestore denied this request. Publish the scoped development rules in docs/firestore.rules, then retry.";
        else if (status == 404) message = "Firestore database or requested resource was not found. Check the Firebase project and database setup.";
        else if (status == 409 || status == 400) message = "A demo record could not be created. Refresh the page and try again.";
        return new FirestoreException(message, status);
    }
}
