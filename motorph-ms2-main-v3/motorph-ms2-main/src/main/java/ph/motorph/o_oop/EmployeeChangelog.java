package ph.motorph.o_oop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EmployeeChangelog {
    public static final class Entry {
        public final String at, employeeId, field, before, after, by, via;

        Entry(String at, String employeeId, String field, String before, String after, String by, String via) {
            this.at = at;
            this.employeeId = employeeId;
            this.field = field;
            this.before = before;
            this.after = after;
            this.by = by;
            this.via = via;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    
    public synchronized void record(String at, String employeeId, String field, String before, String after, String by, String via) {
        String from = before == null ? "" : before, to = after == null ? "" : after;
        if (Objects.equals(from, to)) return;
        entries.add(new Entry(at, employeeId, field, from, to, by == null || by.isEmpty() ? "system" : by, via));
    }

    
    public synchronized List<Entry> forEmployee(String employeeId, int limit) {
        List<Entry> found = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && found.size() < limit; i--) {
            if (employeeId.isEmpty() || entries.get(i).employeeId.equals(employeeId)) found.add(entries.get(i));
        }
        return found;
    }
}
