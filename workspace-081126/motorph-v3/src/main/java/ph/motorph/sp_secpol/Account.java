package ph.motorph.sp_secpol;

/**
 * A sign-in account: an employee number, the stored Argon2id hash and the first-login flag.
 *
 * The employee number is the login name. There is no email, no external identity provider, and no second account
 * per role — one person, one account, whatever their role.
 */
public final class Account {
    public final String employeeId;
    private String passwordHash;
    private boolean mustChangePassword;
    private int failedAttempts;
    private String lockedUntil;        // ISO instant, empty when not locked
    private String lastSignIn;         // ISO instant, empty when never

    public Account(String employeeId, String passwordHash, boolean mustChangePassword) {
        this.employeeId = employeeId;
        this.passwordHash = passwordHash;
        this.mustChangePassword = mustChangePassword;
        this.lockedUntil = "";
        this.lastSignIn = "";
    }

    public String passwordHash() { return passwordHash; }
    public boolean mustChangePassword() { return mustChangePassword; }
    public int failedAttempts() { return failedAttempts; }
    public String lockedUntil() { return lockedUntil; }
    public String lastSignIn() { return lastSignIn; }

    void replaceHash(String newHash) { this.passwordHash = newHash; this.mustChangePassword = false; }

    void recordFailure(int maxAttempts, java.time.Instant now, java.time.Duration lockFor) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockFor).toString();
            failedAttempts = 0;
        }
    }

    void recordSuccess(java.time.Instant now) {
        failedAttempts = 0;
        lockedUntil = "";
        lastSignIn = now.toString();
    }

    boolean locked(java.time.Instant now) {
        if (lockedUntil == null || lockedUntil.isEmpty()) return false;
        return now.isBefore(java.time.Instant.parse(lockedUntil));
    }
}
