package ph.motorph.l_log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * System Admin audit trail (design doc "audit logs", "Review Changes"): one entry per change request and per sign-in
 * attempt, with who, what (method and path, never the request body, so no personal data), the target and the result.
 * Keeps the newest KEEP entries; older ones are handed to the archiver (data/archive) as the requirements' "archival of older logs".
 */
public final class AuditLog {
    static final int KEEP = 1000;
    static final int ARCHIVE_BATCH = 500;

    public static final class Entry {
        public final String at, actor, method, path, target;
        public final int status;

        Entry(String at, String actor, String method, String path, String target, int status) {
            this.at = at;
            this.actor = actor;
            this.method = method;
            this.path = path;
            this.target = target;
            this.status = status;
        }
    }

    /** Receives the oldest entries when the log is full; returns the archive's name, or null if they could not be archived. */
    public interface Archiver {
        String archive(List<Entry> oldest);
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<String> archives = new ArrayList<>();

    public synchronized void record(String at, String actor, String method, String path, String target, int status, Archiver archiver) {
        entries.add(new Entry(at, actor, method, path, target == null ? "" : target, status));
        if (entries.size() > KEEP) {
            List<Entry> oldest = new ArrayList<>(entries.subList(0, ARCHIVE_BATCH));
            String name = archiver == null ? null : archiver.archive(oldest);
            // Without a data file (memory-only demo) the oldest entries are simply dropped.
            entries.subList(0, ARCHIVE_BATCH).clear();
            if (name != null) archives.add(name);
        }
    }

    /** Newest first; empty filters match everything. */
    public synchronized List<Entry> search(String actor, String pathContains, boolean failuresOnly, int limit) {
        List<Entry> found = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && found.size() < limit; i--) {
            Entry entry = entries.get(i);
            if (!actor.isEmpty() && !entry.actor.equals(actor) && !entry.target.equals(actor)) continue;
            if (!pathContains.isEmpty() && !entry.path.contains(pathContains)) continue;
            if (failuresOnly && entry.status < 400) continue;
            found.add(entry);
        }
        return found;
    }

    public synchronized int size() { return entries.size(); }

    public synchronized List<String> archives() { return Collections.unmodifiableList(new ArrayList<>(archives)); }
}
