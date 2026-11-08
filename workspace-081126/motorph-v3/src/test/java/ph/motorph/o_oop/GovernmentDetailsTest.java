package ph.motorph.o_oop;

import ph.motorph.o_oop.GovernmentDetails;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Formats, masking, change approval and same-day full-view grants. All numbers are fake. */
public final class GovernmentDetailsTest {
    private static int checks;

    public static void main(String[] args) {
        check("12-3456789-0".equals(GovernmentDetails.display("sss", GovernmentDetails.normalize("sss", "1234567890"), false)), "SSS format");
        check("••-••••789-0".equals(GovernmentDetails.display("sss", "1234567890", true)), "masked SSS keeps the last 4 digits");
        check("••-••••••901-2".equals(GovernmentDetails.display("philHealth", "123456789012", true)), "masked PhilHealth");
        check("••••-••••-9012".equals(GovernmentDetails.display("pagIbig", "123456789012", true)), "masked Pag-IBIG MID");
        check("123-456-789".equals(GovernmentDetails.display("tin", "123456789", false)) && "123-456-789-000".equals(GovernmentDetails.display("tin", "123456789000", false)),
            "TIN with 9 or 12 digits");
        check("".equals(GovernmentDetails.normalize("tin", " ")), "blank clears a number");
        expect(() -> GovernmentDetails.normalize("sss", "12-345"), "SSS needs 10 digits");
        expect(() -> GovernmentDetails.normalize("tin", "1234567890"), "TIN must be 9 or 12 digits");
        expect(() -> GovernmentDetails.normalize("pagIbig", "1234-5678-901A"), "digits only");

        GovernmentDetails store = new GovernmentDetails();
        LocalDate day = LocalDate.parse("2026-11-06");
        Map<String, String> change = new LinkedHashMap<>();
        change.put("pagIbig", "1234-5678-9012");
        GovernmentDetails.Request proposal = store.proposeChange("DEMO-004", change, "New MID", "DEMO-002");
        check("".equals(store.get("DEMO-004").digits.get("pagIbig")), "a proposed change does nothing until approved");
        store.decide(proposal.id, "approve", "DEMO-005", day);
        check("123456789012".equals(store.get("DEMO-004").digits.get("pagIbig")), "approval applies the change");
        expect(() -> store.decide(proposal.id, "reject", "DEMO-005", day), "decided once");

        GovernmentDetails.Request access = store.requestAccess("DEMO-004", "Remittance check", "DEMO-001");
        check(!store.hasAccess("DEMO-001", "DEMO-004", day), "no full view before approval");
        store.decide(access.id, "approve", "DEMO-005", day);
        check(store.hasAccess("DEMO-001", "DEMO-004", day), "approved full view for that person");
        check(!store.hasAccess("DEMO-001", "DEMO-004", day.plusDays(1)), "the grant ends with the day");
        check(!store.hasAccess("DEMO-002", "DEMO-004", day) && !store.hasAccess("DEMO-001", "DEMO-003", day), "grants are per viewer and per employee");
        expect(() -> store.requestAccess("DEMO-004", " ", "DEMO-001"), "access needs a reason");
        check(store.audit().size() == 4 && "ACCESS_APPROVED".equals(store.audit().get(0).action), "every request and decision is logged");
        System.out.println("PASS: " + checks + " government-details checks");
    }

    private static void expect(Runnable action, String message) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            check(true, message);
            return;
        }
        throw new AssertionError(message + ": nothing was thrown");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
