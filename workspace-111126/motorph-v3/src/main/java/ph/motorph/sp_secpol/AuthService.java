package ph.motorph.sp_secpol;

import java.util.Optional;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.OrgChart;

public final class AuthService {
    public static final String COOKIE = "motorph_session";

    private final CredentialStore credentials;
    private final SessionStore sessions;
    private final OrgChart chart;
    private final TwoFactor twoFactor;
    private int codeMinutes = 5;

    public AuthService(CredentialStore credentials, SessionStore sessions, OrgChart chart) {
        this(credentials, sessions, chart, new TwoFactor(java.time.Clock.systemDefaultZone()));
    }

    public AuthService(CredentialStore credentials, SessionStore sessions, OrgChart chart, TwoFactor twoFactor) {
        this.credentials = credentials;
        this.sessions = sessions;
        this.chart = chart;
        this.twoFactor = twoFactor;
    }

    public TwoFactor twoFactor() { return twoFactor; }
    public void setCodeMinutes(int minutes) { if (minutes > 0) this.codeMinutes = minutes; }
    public int codeMinutes() { return codeMinutes; }

    public SessionStore sessions() { return sessions; }
    public CredentialStore credentials() { return credentials; }

    
    public static final class SignIn {
        public final String token, employeeId, role;
        public final boolean defaultPassword;
        public final String error;

        private SignIn(String token, String employeeId, String role, boolean defaultPassword, String error) {
            this.token = token; this.employeeId = employeeId; this.role = role;
            this.defaultPassword = defaultPassword; this.error = error;
        }
        static SignIn failed(String error) { return new SignIn("", "", "", false, error); }
        public boolean ok() { return error.isEmpty(); }
    }

    
    /** Step one: the password. Returns a challenge, never a session. */
    public SignIn signIn(String employeeId, String password) {
        String id = employeeId == null ? "" : employeeId.trim();
        if (!id.matches("[0-9]{5}")) return SignIn.failed("Employee numbers are five digits, for example 10015.");
        CredentialStore.Result result = credentials.signIn(id, password);
        if (!result.ok) {
            return SignIn.failed("locked".equals(result.reason)
                ? "Too many incorrect attempts. Try again in a few minutes."
                : "inactive".equals(result.reason)
                    ? "This account is deactivated. Contact IT to reactivate it."
                    : "The employee number or password is incorrect.");
        }
        Employee employee = chart.employee(id).orElse(null);
        if (employee == null) return SignIn.failed("The employee number or password is incorrect.");
        TwoFactor.Started started = twoFactor.start(id, "", codeMinutes);
        return new SignIn(started.challengeId, id, AccessPolicy.roleOf(id).label, result.account.usesDefaultPassword(), "");
    }

    /** Step two: the code. This is where the session is actually issued. */
    public SignIn verifyCode(String challengeId, String code) {
        String employeeId;
        try {
            employeeId = twoFactor.verify(challengeId, code);
        } catch (IllegalArgumentException | IllegalStateException refused) {
            return SignIn.failed(refused.getMessage());
        }
        if (!credentials.isEnabled(employeeId))
            return SignIn.failed("This account is deactivated. Contact IT to reactivate it.");
        Employee employee = chart.employee(employeeId).orElse(null);
        if (employee == null) return SignIn.failed("The employee number or password is incorrect.");
        String token = sessions.issue(employeeId);
        boolean defaultPassword = credentials.account(employeeId).map(Account::usesDefaultPassword).orElse(false);
        return new SignIn(token, employeeId, AccessPolicy.roleOf(employeeId).label, defaultPassword, "");
    }

    
    public Optional<Employee> currentEmployee(String token) {
        return sessions.resolve(token).flatMap(record -> chart.employee(record.employeeId));
    }

    public Optional<SessionRecord> currentSession(String token) { return sessions.resolve(token); }

    public void signOut(String token) { sessions.revoke(token); }

    
    public String changePassword(String token, String currentPassword, String newPassword) {
        Optional<SessionRecord> record = sessions.resolve(token);
        if (!record.isPresent()) return null;
        String employeeId = record.get().employeeId;
        if (!credentials.changePassword(employeeId, currentPassword, newPassword)) return null;
        sessions.revokeAllFor(employeeId);
        return sessions.issue(employeeId);
    }

    
    public boolean resetPassword(String employeeId) {
        boolean reset = credentials.resetToInitial(employeeId);
        if (reset) sessions.revokeAllFor(employeeId);
        return reset;
    }

    public boolean setAccountEnabled(String employeeId, boolean enabled) {
        boolean updated = credentials.setEnabled(employeeId, enabled);
        if (updated && !enabled) {
            sessions.revokeAllFor(employeeId);
            twoFactor.revokeFor(employeeId);
        }
        return updated;
    }

    public int activeSessions() { return sessions.size(); }
}
