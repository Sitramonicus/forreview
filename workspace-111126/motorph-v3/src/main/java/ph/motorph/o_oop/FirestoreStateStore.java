package ph.motorph.o_oop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The same state document the file backend writes, stored in Firestore instead.
 *
 * Firestore caps a document at 1 MiB and the attendance store alone is ~940 KB, so the
 * stores are split across documents rather than kept in one blob:
 *
 *   motorphState/_meta and _index        snapshot metadata and chunk counts
 *   motorphStateChunks/{store}_0000      a slice of one store, joined back on load
 *
 * Every value is stored as a JSON string inside a single field. That keeps Firestore's
 * typed-value encoding out of the picture entirely -- what comes back is byte-identical
 * to what went in, so the pinned @SerializedName keys keep working untouched.
 */
public final class FirestoreStateStore implements StateStore {

    private static final int FORMAT_VERSION = 1;
    // Leave room for non-ASCII UTF-8 and never split a surrogate pair.
    private static final int CHUNK_CHARS = 200_000;
    private static final String ROOT = "motorphState";
    // Firestore document paths must alternate collection/document, so a chunk cannot live at
    // motorphState/chunks/{id} (three segments after documents, which it rejects with
    // "lacks / at index N"). Chunks therefore live in their own top-level collection.
    private static final String CHUNKS = "motorphStateChunks";

    private final FirestoreRestClient fs_client;
    private boolean observedMeta, metaExists;
    private String metaUpdateTime;
    private final java.util.Map<String, String> cachedChunks = new java.util.LinkedHashMap<>();
    private final Gson fs_gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, text(LocalDate::parse))
        .registerTypeAdapter(LocalTime.class, text(LocalTime::parse))
        .registerTypeAdapter(LocalDateTime.class, text(LocalDateTime::parse))
        .registerTypeAdapter(YearMonth.class, text(YearMonth::parse))
        .disableHtmlEscaping()
        .create();

    public FirestoreStateStore(FirestoreRestClient fs_client) {
        this.fs_client = fs_client;
    }

    private interface Parser<T> { T apply(String value); }

    private static <T> com.google.gson.TypeAdapter<T> text(Parser<T> parse) {
        return new com.google.gson.TypeAdapter<T>() {
            @Override public void write(com.google.gson.stream.JsonWriter out, T value) throws IOException {
                if (value == null) { out.nullValue(); return; }
                out.value(value.toString());
            }
            @Override public T read(com.google.gson.stream.JsonReader in) throws IOException {
                if (in.peek() == com.google.gson.stream.JsonToken.NULL) { in.nextNull(); return null; }
                return parse.apply(in.nextString());
            }
        }.nullSafe();
    }

    @Override public Gson gson() { return fs_gson; }

    @Override public String backend() { return "firestore"; }

    @Override public String describe() { return "Firestore (" + ROOT + " collection)"; }

    @Override public Optional<JsonObject> load() throws IOException {
        JsonObject meta = readObject("_meta");
        if (meta == null) return Optional.empty();
        int format = meta.has("format") ? meta.get("format").getAsInt() : FORMAT_VERSION;
        if (format != FORMAT_VERSION) throw new IOException("Unknown Firestore state format: " + format);

        String loadedVersion = metaUpdateTime;
        java.util.Map<String, String> loadedChunks = new java.util.LinkedHashMap<>();
        JsonObject stores = new JsonObject();
        JsonObject index = readObject("_index");
        if (index == null) return Optional.of(stores);

        for (String storeName : index.keySet()) {
            int chunks = index.get(storeName).getAsInt();
            StringBuilder joined = new StringBuilder();
            for (int part = 0; part < chunks; part++) {
                JsonObject chunk = readObject("chunks/" + storeName + "_" + String.format("%04d", part));
                if (chunk == null || !chunk.has("json")) {
                    throw new IOException("Firestore state is missing chunk " + part + " of " + storeName);
                }
                String value = chunk.get("json").getAsString();
                loadedChunks.put(storeName + "_" + String.format("%04d", part), value);
                joined.append(value);
            }
            stores.add(storeName, JsonParser.parseString(joined.toString()));
        }
        if (loadedVersion != null) {
            readObject("_meta");
            if (!loadedVersion.equals(metaUpdateTime))
                throw new IOException("Firestore changed during loading; restart and try again.");
        }
        cachedChunks.clear();
        cachedChunks.putAll(loadedChunks);
        return Optional.of(stores);
    }

    @Override public synchronized void save(JsonObject stores) throws IOException {
        if (!observedMeta) readObject("_meta");
        JsonObject previousIndex = readObject("_index");
        JsonObject index = new JsonObject();
        java.util.Map<String, JsonObject> updates = new java.util.LinkedHashMap<>();
        java.util.List<String> deletes = new ArrayList<>();
        java.util.Map<String, String> nextChunks = new java.util.LinkedHashMap<>();
        for (String storeName : stores.keySet()) {
            String json = fs_gson.toJson(stores.get(storeName));
            int from = 0, part = 0;
            do {
                int to = Math.min(json.length(), from + CHUNK_CHARS);
                if (to < json.length() && to > from && Character.isHighSurrogate(json.charAt(to - 1))) to--;
                String id = storeName + "_" + String.format("%04d", part++);
                String value = json.substring(from, to);
                nextChunks.put(id, value);
                if (!value.equals(cachedChunks.get(id)))
                    updates.put(CHUNKS + "/" + id, typedFields(single("json", value)));
                from = to;
            } while (from < json.length());
            index.addProperty(storeName, part);
        }
        if (previousIndex != null) for (String name : previousIndex.keySet()) {
            int previous = previousIndex.get(name).getAsInt();
            int current = index.has(name) ? index.get(name).getAsInt() : 0;
            for (int part = current; part < previous; part++)
                deletes.add(CHUNKS + "/" + name + "_" + String.format("%04d", part));
        }
        updates.put(ROOT + "/_index", typedFields(index));
        updates.put(ROOT + "/_meta", typedFields(meta(Instant.now().toString())));
        int metaResultIndex = updates.size() - 1;
        JsonObject result = fs_client.commitDocuments(updates, deletes, ROOT + "/_meta", metaUpdateTime, metaExists);
        JsonArray results = result.has("writeResults") ? result.getAsJsonArray("writeResults") : new JsonArray();
        if (results.size() > metaResultIndex && results.get(metaResultIndex).getAsJsonObject().has("updateTime")) {
            metaUpdateTime = results.get(metaResultIndex).getAsJsonObject().get("updateTime").getAsString();
            metaExists = true;
        } else {
            observedMeta = false;
            readObject("_meta");
        }
        cachedChunks.clear();
        cachedChunks.putAll(nextChunks);
    }

    private JsonObject typedFields(JsonObject flat) {
        JsonObject typed = new JsonObject();
        for (String key : flat.keySet()) {
            JsonObject value = new JsonObject();
            JsonElement raw = flat.get(key);
            if (raw.getAsJsonPrimitive().isNumber()) value.addProperty("integerValue", raw.getAsLong());
            else value.addProperty("stringValue", raw.getAsString());
            typed.add(key, value);
        }
        return typed;
    }

    @Override public synchronized String backupNow(String reason) throws IOException {
        JsonObject state = load().orElseThrow(() -> new IOException("There is no state to back up."));
        String name = "backup-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().replace("-", "");
        JsonObject index = new JsonObject();
        java.util.Map<String, JsonObject> updates = new java.util.LinkedHashMap<>();
        for (String store : state.keySet()) {
            String json = fs_gson.toJson(state.get(store));
            int from = 0, part = 0;
            do {
                int to = Math.min(json.length(), from + CHUNK_CHARS);
                if (to < json.length() && to > from && Character.isHighSurrogate(json.charAt(to - 1))) to--;
                String id = name + "_" + store + "_" + String.format("%04d", part++);
                updates.put("motorphBackupChunks/" + id, typedFields(single("json", json.substring(from, to))));
                from = to;
            } while (from < json.length());
            index.addProperty(store, part);
        }
        JsonObject header = new JsonObject();
        header.addProperty("index", fs_gson.toJson(index));
        header.addProperty("reason", reason == null ? "manual" : reason);
        header.addProperty("savedAt", Instant.now().toString());
        updates.put("motorphBackups/" + name, typedFields(header));
        fs_client.commitDocuments(updates, new ArrayList<>(), null, null, false);
        return name;
    }

    @Override public synchronized List<String> listBackups() throws IOException {
        List<String> names = new ArrayList<>();
        for (JsonObject doc : fs_client.listDocuments("motorphBackups")) {
            if (!doc.has("name")) continue;
            String path = doc.get("name").getAsString();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.matches("backup-[0-9]+-[0-9a-f]{32}")) names.add(name);
        }
        names.sort(java.util.Collections.reverseOrder());
        return names;
    }

    @Override public synchronized JsonObject loadBackup(String name) throws IOException {
        if (name == null || !name.matches("backup-[0-9]+-[0-9a-f]{32}")) throw new IOException("Invalid backup name.");
        JsonObject header = readFlat("motorphBackups", name);
        if (header == null || !header.has("index")) throw new IOException("Backup not found.");
        JsonObject index = JsonParser.parseString(header.get("index").getAsString()).getAsJsonObject();
        JsonObject state = new JsonObject();
        for (String store : index.keySet()) {
            int count = index.get(store).getAsInt();
            StringBuilder joined = new StringBuilder();
            for (int part = 0; part < count; part++) {
                JsonObject chunk = readFlat("motorphBackupChunks", name + "_" + store + "_" + String.format("%04d", part));
                if (chunk == null || !chunk.has("json")) throw new IOException("Backup is missing a data chunk.");
                joined.append(chunk.get("json").getAsString());
            }
            state.add(store, JsonParser.parseString(joined.toString()));
        }
        return state;
    }

    @Override public synchronized String writeArchive(String prefix, JsonElement content) throws IOException {
        String name = "archive-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().replace("-", "");
        java.util.Map<String, JsonObject> updates = new java.util.LinkedHashMap<>();
        updates.put("motorphArchives/" + name, typedFields(single("json", fs_gson.toJson(content))));
        fs_client.commitDocuments(updates, new ArrayList<>(), null, null, false);
        return name;
    }

    private JsonObject readFlat(String collection, String id) throws IOException {
        JsonObject raw = fs_client.getDocument(collection, id);
        if (raw == null) return null;
        JsonObject fields = raw.has("fields") ? raw.getAsJsonObject("fields") : new JsonObject();
        JsonObject result = new JsonObject();
        for (String key : fields.keySet()) {
            JsonObject typed = fields.getAsJsonObject(key);
            if (typed.has("stringValue")) result.addProperty(key, typed.get("stringValue").getAsString());
            else if (typed.has("integerValue")) result.addProperty(key, typed.get("integerValue").getAsLong());
        }
        return result;
    }

    private JsonObject meta(String savedAt) {
        JsonObject object = new JsonObject();
        object.addProperty("format", FORMAT_VERSION);
        object.addProperty("savedAt", savedAt);
        return object;
    }

    private JsonObject single(String field, String value) {
        JsonObject object = new JsonObject();
        object.addProperty(field, value);
        return object;
    }

    private JsonObject readObject(String documentId) throws IOException {
        String path = ROOT;
        String id = documentId;
        if (documentId.startsWith("chunks/")) {
            path = CHUNKS;
            id = documentId.substring("chunks/".length());
        }
        JsonObject raw = fs_client.getDocument(path, id);
        if ("_meta".equals(documentId)) {
            observedMeta = true;
            metaExists = raw != null;
            metaUpdateTime = raw != null && raw.has("updateTime") ? raw.get("updateTime").getAsString() : null;
        }
        if (raw == null) return null;
        JsonObject fields = raw.has("fields") ? raw.getAsJsonObject("fields") : new JsonObject();
        JsonObject flat = new JsonObject();
        for (String key : fields.keySet()) {
            JsonObject typed = fields.getAsJsonObject(key);
            if (typed.has("stringValue")) flat.addProperty(key, typed.get("stringValue").getAsString());
            else if (typed.has("integerValue")) flat.addProperty(key, typed.get("integerValue").getAsLong());
        }
        return flat;
    }

    private void writeObject(String documentId, JsonObject flat) throws IOException {
        JsonObject typed = new JsonObject();
        for (String key : flat.keySet()) {
            JsonObject value = new JsonObject();
            JsonElement raw = flat.get(key);
            if (raw.getAsJsonPrimitive().isNumber()) value.addProperty("integerValue", raw.getAsLong());
            else value.addProperty("stringValue", raw.getAsString());
            typed.add(key, value);
        }
        String path = ROOT;
        String id = documentId;
        if (documentId.startsWith("chunks/")) {
            path = CHUNKS;
            id = documentId.substring("chunks/".length());
        }
        if (!fs_client.setDocument(path, id, typed)) {
            throw new IOException("Firestore refused to write " + documentId);
        }
    }

    /**
     * Chunk ids are positional, so a store that shrinks from three chunks to one leaves
     * orphans behind. Delete the tail so the console shows exactly what is live.
     */
    private void pruneStaleChunks(JsonObject previous, JsonObject index) throws IOException {
        if (previous == null) return;
        for (String storeName : previous.keySet()) {
            int was = previous.get(storeName).getAsInt();
            int now = index.has(storeName) ? index.get(storeName).getAsInt() : 0;
            for (int part = now; part < was; part++) {
                fs_client.deleteDocument(CHUNKS, storeName + "_" + String.format("%04d", part));
            }
        }
    }
}
