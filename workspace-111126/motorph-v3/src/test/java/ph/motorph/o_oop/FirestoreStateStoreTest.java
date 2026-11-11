package ph.motorph.o_oop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exercises FirestoreStateStore against a stand-in Firestore, so the save/load path can be
 * proven without a project, a service account or a network. The fake implements only the two
 * calls the store makes: create a document, and list a collection.
 */
public final class FirestoreStateStoreTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        FakeFirestore fake = new FakeFirestore();
        fake.start();
        try {
            String apiRoot = "http://127.0.0.1:" + fake.port() + "/v1/projects/";
            FirestoreRestClient client = new FirestoreRestClient("demo-project", "uid-under-test", "fake-token", apiRoot);
            FirestoreStateStore store = new FirestoreStateStore(client);

            check(store.load().isEmpty(), "an empty Firestore loads as nothing, not an error");

            JsonObject stores = new JsonObject();
            JsonObject employees = new JsonObject();
            employees.addProperty("10001", "Manuel III Garcia");
            stores.add("employees", employees);

            JsonObject attendance = new JsonObject();
            StringBuilder big = new StringBuilder();
            for (int i = 0; i < 120_000; i++) big.append("row").append(i).append(';');
            attendance.addProperty("blob", big.toString());
            stores.add("attendance", attendance);

            long rawBytes = big.length();
            check(rawBytes > 700_000, "the sample attendance store is big enough to need chunking (" + rawBytes + " bytes)");

            store.save(stores);
            check(fake.documentCount() >= 3, "save wrote meta, index and at least one chunk (" + fake.documentCount() + " docs)");
            if (fake.chunkCount("attendance") == 0) System.out.println("DEBUG keys: " + fake.documentIds());
            check(fake.chunkCount("attendance") >= 2, "attendance was split across " + fake.chunkCount("attendance") + " chunks");

            Optional<JsonObject> loaded = store.load();
            check(loaded.isPresent(), "a saved state loads back");
            JsonObject back = loaded.get();
            check(back.has("employees"), "employees survive the round trip");
            check("Manuel III Garcia".equals(back.getAsJsonObject("employees").get("10001").getAsString()),
                "a small store comes back byte for byte");
            check(back.has("attendance"), "attendance survives the round trip");
            check(big.toString().equals(back.getAsJsonObject("attendance").get("blob").getAsString()),
                "a chunked store rejoins to exactly what went in (" + rawBytes + " bytes)");

            JsonObject second = new JsonObject();
            second.addProperty("note", "second save");
            store.save(second);
            Optional<JsonObject> reloaded = store.load();
            check(reloaded.isPresent() && reloaded.get().has("note"), "a second save replaces the first");
            check(!reloaded.get().has("attendance"), "the old store is gone after a full save, not merged in");
            check(fake.chunkCount("attendance") == 0,
                "chunks for a store that no longer exists are deleted, not left orphaned in the console");

            // Saving twice must not fail. The REST client used to POST only, which real Firestore
            // answers with ALREADY_EXISTS, so the second save of a live app would have been lost.
            JsonObject third = new JsonObject();
            third.addProperty("note", "third save");
            store.save(third);
            store.save(third);
            check(fake.lastWriteCount == 2, "an unchanged snapshot writes only index and metadata");
            check(store.load().isPresent(), "a repeated save overwrites in place instead of failing on ALREADY_EXISTS");

            // Shrinking a chunked store must not leave the tail behind for load() to rejoin.
            JsonObject bigAgain = new JsonObject();
            JsonObject rows = new JsonObject();
            rows.addProperty("blob", big.toString());
            bigAgain.add("attendance", rows);
            store.save(bigAgain);
            JsonObject smallAgain = new JsonObject();
            JsonObject tinyRows = new JsonObject();
            tinyRows.addProperty("blob", "one row");
            smallAgain.add("attendance", tinyRows);
            store.save(smallAgain);
            check(fake.chunkCount("attendance") == 1,
                "shrinking from several chunks to one deletes the surplus (" + fake.chunkCount("attendance") + " left)");
            check("one row".equals(store.load().get().getAsJsonObject("attendance").get("blob").getAsString()),
                "the shrunken store loads back without stale tail data");

            FirestoreStateStore stale = new FirestoreStateStore(client);
            stale.load();
            JsonObject newer = new JsonObject(); newer.addProperty("note", "newer commit");
            store.save(newer);
            JsonObject conflicting = new JsonObject(); conflicting.addProperty("note", "stale writer");
            try { stale.save(conflicting); throw new AssertionError("stale writer was accepted"); }
            catch (FirestoreException expected) { check(expected.getStatusCode() == 412, "stale writer is rejected"); }
            check("newer commit".equals(store.load().get().get("note").getAsString()),
                "a rejected stale commit does not partially overwrite the newer state");
            fake.failNextCommit = true;
            try { store.save(conflicting); throw new AssertionError("failed commit was accepted"); }
            catch (FirestoreException expected) { check(expected.getStatusCode() == 503, "commit failure is surfaced"); }
            check("newer commit".equals(store.load().get().get("note").getAsString()),
                "a failed commit leaves the previous snapshot intact");
            String backup = store.backupNow("manual");
            check(store.listBackups().contains(backup), "backup is listed");
            check("newer commit".equals(store.loadBackup(backup).get("note").getAsString()),
                "backup loads its original snapshot");
            store.save(conflicting);
            check("newer commit".equals(store.loadBackup(backup).get("note").getAsString()),
                "later state changes do not alter the backup");
        } finally {
            fake.stop();
        }
        System.out.println("FirestoreStateStoreTest: " + checks + " checks passed");
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAIL " + what);
            throw new AssertionError(what);
        }
        checks++;
    }

    private static final class FakeFirestore {
        private final HttpServer server;
        private final Map<String, JsonObject> documents = new LinkedHashMap<>();
        private final Map<String, String> updateTimes = new LinkedHashMap<>();
        private int version;
        boolean failNextCommit;
        int lastWriteCount;

        FakeFirestore() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/projects/", this::handle);
        }

        void start() { server.start(); }
        void stop() { server.stop(0); }
        int port() { return server.getAddress().getPort(); }
        int documentCount() { return documents.size(); }

        java.util.List<String> documentIds() { return new java.util.ArrayList<>(documents.keySet()); }

        int chunkCount(String store) {
            int count = 0;
            for (String id : documents.keySet()) {
                if (id.contains("motorphStateChunks/" + store + "_")) count++;
            }
            return count;
        }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery();
            int marker = path.indexOf("/documents");
            String tail = marker < 0 ? "" : path.substring(marker + "/documents".length());
            tail = tail.startsWith("/") ? tail.substring(1) : tail;
            final String base = tail;

            if (path.endsWith("/documents:commit") && "POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject commit = JsonParser.parseString(body).getAsJsonObject();
                if (failNextCommit) { failNextCommit = false; reply(exchange, 503, "{}"); return; }
                lastWriteCount = commit.getAsJsonArray("writes").size();
                Map<String, JsonObject> next = new LinkedHashMap<>(documents);
                for (com.google.gson.JsonElement element : commit.getAsJsonArray("writes")) {
                    JsonObject write = element.getAsJsonObject();
                    if (write.has("update")) {
                        JsonObject doc = write.getAsJsonObject("update");
                        String name = doc.get("name").getAsString();
                        String key = name.substring(name.indexOf("/documents/") + "/documents/".length());
                        if (key.split("/").length % 2 != 0) { reply(exchange, 400, "{}"); return; }
                        if (write.has("currentDocument")) {
                            JsonObject condition = write.getAsJsonObject("currentDocument");
                            if (condition.has("updateTime") && !condition.get("updateTime").getAsString().equals(updateTimes.get(key))) {
                                reply(exchange, 412, "{}"); return;
                            }
                            if (condition.has("exists") && condition.get("exists").getAsBoolean() != documents.containsKey(key)) {
                                reply(exchange, 412, "{}"); return;
                            }
                        }
                        next.put(key, doc.getAsJsonObject("fields"));
                    } else if (write.has("delete")) {
                        String name = write.get("delete").getAsString();
                        next.remove(name.substring(name.indexOf("/documents/") + "/documents/".length()));
                    }
                }
                documents.clear();
                documents.putAll(next);
                String timestamp = "2026-01-01T00:00:" + String.format("%02d", ++version) + "Z";
                com.google.gson.JsonArray results = new com.google.gson.JsonArray();
                for (com.google.gson.JsonElement element : commit.getAsJsonArray("writes")) {
                    JsonObject write = element.getAsJsonObject();
                    JsonObject item = new JsonObject();
                    if (write.has("update")) {
                        String name = write.getAsJsonObject("update").get("name").getAsString();
                        String key = name.substring(name.indexOf("/documents/") + "/documents/".length());
                        updateTimes.put(key, timestamp);
                        item.addProperty("updateTime", timestamp);
                    }
                    results.add(item);
                }
                JsonObject response = new JsonObject(); response.add("writeResults", results);
                reply(exchange, 200, response.toString());
                return;
            }

            // Firestore requires collection/document/collection/document... A path with an odd
            // number of segments is a document name it rejects with HTTP 400. The real API does
            // this too; a fake that tolerates it hides broken layouts.
            if (!base.isEmpty()) {
                String[] segments = base.split("/");
                if (segments.length % 2 == 1 && !"GET".equals(exchange.getRequestMethod())) {
                    reply(exchange, 400, "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":"
                        + "\"Document name lacks / at index 80.\"}}");
                    return;
                }
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                String documentId = valueOf(query, "documentId");
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
                documents.put(tail + "/" + documentId, parsed.getAsJsonObject("fields"));
                reply(exchange, 200, "{\"name\":\"projects/demo/databases/(default)/documents/" + tail + "/" + documentId + "\"}");
                return;
            }

            if ("PATCH".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
                documents.put(tail, parsed.getAsJsonObject("fields"));
                reply(exchange, 200, "{\"name\":\"projects/demo/databases/(default)/documents/" + tail + "\"}");
                return;
            }

            if ("DELETE".equals(exchange.getRequestMethod())) {
                documents.remove(tail);
                reply(exchange, 200, "{}");
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                // Single document read: no trailing collection segment, id exists in the map.
                if (documents.containsKey(tail)) {
                    JsonObject document = new JsonObject();
                    document.addProperty("name", "projects/demo/databases/(default)/documents/" + tail);
                    document.add("fields", documents.get(tail));
                    if (updateTimes.containsKey(tail)) document.addProperty("updateTime", updateTimes.get(tail));
                    reply(exchange, 200, document.toString());
                    return;
                }
                List<String> names = new ArrayList<>();
                for (Map.Entry<String, JsonObject> entry : documents.entrySet()) {
                    if (!entry.getKey().startsWith(tail + "/")) continue;
                    if (entry.getKey().substring(tail.length() + 1).contains("/")) continue;
                    JsonObject document = new JsonObject();
                    document.addProperty("name", "projects/demo/databases/(default)/documents/" + entry.getKey());
                    document.add("fields", entry.getValue());
                    names.add(document.toString());
                }
                boolean isCollection = tail.isEmpty() || documents.keySet().stream()
                    .anyMatch(key -> key.startsWith(base + "/"));
                if (!isCollection) {
                    // A document path with nothing stored there: real Firestore answers 404.
                    reply(exchange, 404, "{\"error\":{\"status\":\"NOT_FOUND\"}}");
                    return;
                }
                reply(exchange, 200, "{\"documents\":[" + String.join(",", names) + "]}");
                return;
            }
            reply(exchange, 405, "{}");
        }

        private static String valueOf(String query, String key) {
            if (query == null) return "";
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals(key)) {
                    return java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                }
            }
            return "";
        }

        private static void reply(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
        }
    }
}
