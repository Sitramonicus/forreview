package ph.motorph.l_log;

import ph.motorph.l_log.AuditLog;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogTest {
    private static int lg_iChecks;

    public static void main(String[] args) {
        AuditLog log = new AuditLog();
        List<List<AuditLog.Entry>> lg_lArchived = new ArrayList<>();
        AuditLog.Archiver lg_arcSink = lg_lOldest -> {
            lg_lArchived.add(lg_lOldest);
            return "audit-test-" + lg_lArchived.size() + ".json";
        };
        for (int lg_iIdx = 1; lg_iIdx <= AuditLog.lg_iKeep; lg_iIdx++) log.record("2026-11-06T09:00", lg_iIdx % 2 == 0 ? "10012" : "10013", "POST", "/api/x/" + lg_iIdx, "", lg_iIdx % 10 == 0 ? 403 : 200, lg_arcSink);
        check(log.size() == 1000 && lg_lArchived.isEmpty(), "up to 1,000 entries are kept");
        log.record("2026-11-06T09:01", "10013", "POST", "/api/x/1001", "", 200, lg_arcSink);
        check(lg_lArchived.size() == 1 && lg_lArchived.get(0).size() == 500 && lg_lArchived.get(0).get(0).lg_sPath.equals("/api/x/1"), "entry 1,001 archives the oldest 500");
        check(log.size() == 501 && log.archives().equals(java.util.Collections.singletonList("audit-test-1.json")), "501 stay, and the archive is listed");
        check(log.search("", "", false, 1).get(0).lg_sPath.equals("/api/x/1001"), "newest first");
        check(log.search("10012", "", false, 1000).stream().allMatch(lg_eRow -> lg_eRow.lg_sActor.equals("10012")), "filter by account");
        check(log.search("", "", true, 1000).size() == 50, "filter to refused requests (every 10th of 501..1000)");
        AuditLog lg_logBare = new AuditLog();
        for (int lg_iIdx = 0; lg_iIdx <= AuditLog.lg_iKeep; lg_iIdx++) lg_logBare.record("t", "a", "POST", "/p", "", 200, null);
        check(lg_logBare.size() == 501 && lg_logBare.archives().isEmpty(), "without a data file the oldest are dropped");
        System.out.println("PASS: " + lg_iChecks + " audit-log checks");
    }

    private static void check(boolean lg_bOk, String lg_sMsg) {
        lg_iChecks++;
        if (!lg_bOk) throw new AssertionError(lg_sMsg);
    }
}
