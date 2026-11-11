package ph.motorph.o_oop;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Where the whole application state lives between requests.
 *
 * Two backends implement it: {@code LocalStateFile} writes one JSON file on disk,
 * {@code FirestoreStateStore} writes the same shape to Firestore. AppServer only ever
 * talks to this interface, so switching is an env-var decision, not a code change.
 */
public interface StateStore {

    Optional<JsonObject> load() throws IOException;

    void save(JsonObject stores) throws IOException;

    Gson gson();

    String describe();

    /** Short key the web UI shows: memory, file or firestore. */
    default String backend() { return "file"; }

    default List<String> listBackups() throws IOException { return new java.util.ArrayList<>(); }

    default JsonObject loadBackup(String name) throws IOException {
        throw new IOException("This state store keeps no backups.");
    }

    default String backupNow(String reason) throws IOException {
        throw new IOException("This state store keeps no backups.");
    }

    default String writeArchive(String prefix, com.google.gson.JsonElement content) throws IOException {
        throw new IOException("This state store keeps no archives.");
    }

    default String backupFolder() { return ""; }

    /** Where the state lives, for the startup banner. */
    default String location() { return describe(); }

    /**
     * A file that could not be parsed is moved aside so the server can start empty. A remote
     * store has nothing to move, so it reports where the unreadable copy was left instead.
     */
    /** Moves unreadable state out of the way. Remote stores have nothing to move: return null. */
    default String setAside() throws IOException {
        return null;
    }
}
