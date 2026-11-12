package ph.motorph.l_log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AuditLog {
    static final int lg_iKeep = 1000;
    static final int lg_iBatch = 500;

    public static final class Entry {
        public final String lg_sAt, lg_sActor, lg_sMethod, lg_sPath, lg_sTarget;
        public final int lg_iStatus;

        Entry(String lg_sAt, String lg_sActor, String lg_sMethod, String lg_sPath, String lg_sTarget, int lg_iStatus) {
            this.lg_sAt = lg_sAt;
            this.lg_sActor = lg_sActor;
            this.lg_sMethod = lg_sMethod;
            this.lg_sPath = lg_sPath;
            this.lg_sTarget = lg_sTarget;
            this.lg_iStatus = lg_iStatus;
        }
    }

    public interface Archiver {
        String archive(List<Entry> oldest);
    }

    private final List<Entry> lg_lEntries = new ArrayList<>();
    private final List<String> lg_lArchives = new ArrayList<>();

    public synchronized void record(String at, String actor, String method, String path, String target, int status, Archiver archiver) {
        lg_lEntries.add(new Entry(at, actor, method, path, target == null ? "" : target, status));
        if (lg_lEntries.size() > lg_iKeep) {
            List<Entry> lg_lOldest = new ArrayList<>(lg_lEntries.subList(0, lg_iBatch));
            String lg_sName = archiver == null ? null : archiver.archive(lg_lOldest);
            lg_lEntries.subList(0, lg_iBatch).clear();
            if (lg_sName != null) lg_lArchives.add(lg_sName);
        }
    }

    public synchronized List<Entry> search(String actor, String pathContains, boolean failuresOnly, int limit) {
        List<Entry> lg_lFound = new ArrayList<>();
        for (int lg_iIdx = lg_lEntries.size() - 1; lg_iIdx >= 0 && lg_lFound.size() < limit; lg_iIdx--) {
            Entry lg_eRow = lg_lEntries.get(lg_iIdx);
            if (!actor.isEmpty() && !lg_eRow.lg_sActor.equals(actor) && !lg_eRow.lg_sTarget.equals(actor)) continue;
            if (!pathContains.isEmpty() && !lg_eRow.lg_sPath.contains(pathContains)) continue;
            if (failuresOnly && lg_eRow.lg_iStatus < 400) continue;
            lg_lFound.add(lg_eRow);
        }
        return lg_lFound;
    }

    public synchronized int size() { return lg_lEntries.size(); }

    public synchronized List<String> archives() { return Collections.unmodifiableList(new ArrayList<>(lg_lArchives)); }
}
