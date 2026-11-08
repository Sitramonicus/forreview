package ph.motorph.sp_secpol;

import java.util.Optional;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.OrgChart;

/**
 * Sign-in, sign-out, password change and the "who is this request" question, in one place.
 *
 * The HTTP layer owns exactly one authentication decision: {@link #currentEmployee}. Everything else — reading
 * permissions, scoping a roster, deciding whether a payslip may be opened — is asked of
 * {@link AccessMatrix} with the employee this service returns. A request that cannot name a live session gets no
 * employee, and therefore no permission.
 */
public final class AuthService {
    public static final String COOKIE = "motorph_session";

    private final CredentialStore credentials;
    private final SessionStore sessions;
    private final OrgChart chart;

    public AuthService(CredentialStore credentials, SessionStore sessions, OrgChart chart) {
        this.credentials = credentials;
        this.sessions = sessions;
        this.chart = chart;
    }

    public SessionStore sessions() { return sessions; }
    public CredentialStore credentials() { return credentials; }

    /** Outcome of a sign-in: a token for the cookie, or a reason the browser may be told. */
    public static final class SignIn {
        public final String token, employeeId, role;
        public final boolean mustChangePassword;
        public final String error;

        private SignIn(String token, String employeeId, String role, boolean mustChangePassword, String error) {
            this.token = token; this.employeeId = employeeId; this.role = role;
            this.mustChangePassword = mustChangePassword; this.error = error;
        }
        static SignIn failed(String error) { return new SignIn("", "", "", false, error); }
        public boolean ok() { return error.isEmpty(); }
    }

    /**
     * Employee ID + password → session.
     *
     * The failure message is the same for an unknown number and a wrong password: "The employee number or password
     * is incorrect." Anything else lets somebody probe the roster one number at a time.
     */
    public SignIn signIn(String employeeId, String password) {
        String id = employeeId == null ? "" : employeeId.trim();
        if (!id.matches("[0-9]{5}")) return SignIn.failed("Employee numbers are five digits, for example 10015.");
        CredentialStore.Result result = credentials.signIn(id, password);
        if (!result.ok) {
            return SignIn.failed("locked".equals(result.reason)
                ? "Too many incorrect attempts. Try again in a few minutes."
                : "The employee number or password is incorrect.");
        }
        Employee employee = chart.employee(id).orElse(null);
        if (employee == null) return SignIn.failed("The employee number or password is incorrect.");
        String token = sessions.issue(id);
        return new SignIn(token, id, chart.roleOf(id).label, result.account.mustChangePassword(), "");
    }

    /** The employee behind a session token, or empty. Resolving also counts as activity. */
    public Optional<Employee> currentEmployee(String token) {
        return sessions.resolve(token).flatMap(record -> chart.employee(record.employeeId));
    }

    public Optional<SessionRecord> currentSession(String token) { return sessions.resolve(token); }

    public void signOut(String token) { sessions.revoke(token); }

    /**
     * Changes the password and ends every other session for that account.
     *
     * The browser that made the change is given a fresh token (session rotation after a credential change, so a
     * token observed before the change is worthless afterwards). Every other session is revoked.
     */
    public String changePassword(String token, String currentPassword, String newPassword) {
        Optional<SessionRecord> record = sessions.resolve(token);
        if (!record.isPresent()) return null;
        String employeeId = record.get().employeeId;
        if (!credentials.changePassword(employeeId, currentPassword, newPassword)) return null;
        sessions.revokeAllFor(employeeId);
        return sessions.issue(employeeId);
    }

    /** System Administrator: restore the initial credential and force a change on next sign-in. */
    public boolean resetPassword(String employeeId) {
        boolean reset = credentials.resetToInitial(employeeId);
        if (reset) sessions.revokeAllFor(employeeId);
        return reset;
    }

    public int activeSessions() { return sessions.size(); }
}
