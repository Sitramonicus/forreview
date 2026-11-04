package motorph.user;

/**
 * Parent class for a system account.
 * <p>
 * Keeps the username and password private (there is no password getter) and
 * shares the login check. Each subclass decides which actions its role may
 * perform through hasPermission(action). The action names are the constants
 * below; the action-to-role matrix is:
 * </p>
 * <pre>
 * Action                 EmployeeUser  PayrollStaff
 * VIEW_OWN_RECORD        Yes           No
 * VIEW_EMPLOYEES         No            Yes
 * MANAGE_EMPLOYEES       No            Yes
 * COMPUTE_PAYROLL        No            Yes
 * VIEW_PAYROLL_SUMMARY   No            Yes
 * SAVE_PAYROLL_RESULTS   No            Yes
 * </pre>
 */
public abstract class User {

    // === ACTIONS (passed to hasPermission) ===
    public static final String VIEW_OWN_RECORD      = "VIEW_OWN_RECORD";
    public static final String VIEW_EMPLOYEES       = "VIEW_EMPLOYEES";
    public static final String MANAGE_EMPLOYEES     = "MANAGE_EMPLOYEES";
    public static final String COMPUTE_PAYROLL      = "COMPUTE_PAYROLL";
    public static final String VIEW_PAYROLL_SUMMARY = "VIEW_PAYROLL_SUMMARY";
    public static final String SAVE_PAYROLL_RESULTS = "SAVE_PAYROLL_RESULTS";

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final String username;
    private final String role;
    private String password;

    protected User(String username, String password, String role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        this.username = username.trim();
        this.role = role;
        setPassword(password);
    }

    public String getUsername() {
        return username;
    }

    /** Returns the role name, for example "Payroll Staff". */
    public String getRole() {
        return role;
    }

    /** Changes the password. Must not be blank and must be at least 8 characters. */
    public final void setPassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        this.password = newPassword;
    }

    /**
     * Checks login credentials. Both fields are required; a blank username or
     * password never authenticates.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        return this.username.equals(username.trim()) && this.password.equals(password);
    }

    /** Returns true if this user's role may perform the action (see the matrix above). */
    public abstract boolean hasPermission(String action);
}
