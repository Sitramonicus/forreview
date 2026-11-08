package ph.motorph.l_log;

import ph.motorph.l_log.AuditLog;
import java.util.ArrayList;
import java.util.List;

/** Audit log: newest first, filters, and archiving the oldest 500 once it passes 1,000 entries. */
public final class AuditLogTest {
    private static int checks;

    public static void main(String[] args) {
        AuditLog log = new AuditLog();
        List<List<AuditLog.Entry>> archived = new ArrayList<>();
        AuditLog.Archiver archiver = oldest -> {
            archived.add(oldest);
            return "audit-test-" + archived.size() + ".json";
        };
        for (int i = 1; i <= AuditLog.KEEP; i++) log.record("2026-11-06T09:00", i % 2 == 0 ? "10012" : "10013", "POST", "/api/x/" + i, "", i % 10 == 0 ? 403 : 200, archiver);
        check(log.size() == 1000 && archived.isEmpty(), "up to 1,000 entries are kept");
        log.record("2026-11-06T09:01", "10013", "POST", "/api/x/1001", "", 200, archiver);
        check(archived.size() == 1 && archived.get(0).size() == 500 && archived.get(0).get(0).path.equals("/api/x/1"), "entry 1,001 archives the oldest 500");
        check(log.size() == 501 && log.archives().equals(java.util.Collections.singletonList("audit-test-1.json")), "501 stay, and the archive is listed");
        check(log.search("", "", false, 1).get(0).path.equals("/api/x/1001"), "newest first");
        check(log.search("10012", "", false, 1000).stream().allMatch(entry -> entry.actor.equals("10012")), "filter by account");
        check(log.search("", "", true, 1000).size() == 50, "filter to refused requests (every 10th of 501..1000)");
        AuditLog memoryOnly = new AuditLog();
        for (int i = 0; i <= AuditLog.KEEP; i++) memoryOnly.record("t", "a", "POST", "/p", "", 200, null);
        check(memoryOnly.size() == 501 && memoryOnly.archives().isEmpty(), "without a data file the oldest are dropped");
        System.out.println("PASS: " + checks + " audit-log checks");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
