package ph.motorph.sp_secpol;

public final class Account {
    public final String acc_sEid;
    private String acc_sHash;
    // Kept as a serialized field for existing files; it records use of the shared default password.
    private boolean acc_bMustChange;
    // Missing from older persisted accounts defaults to false, so older accounts remain active.
    private boolean acc_bDisabled;
    private int acc_iFails;
    private String acc_sLockedUntil;    
    private String acc_sLastSignIn;     

    public Account(String acc_sEid, String acc_sHash, boolean acc_bMustChange) {
        this.acc_sEid = acc_sEid;
        this.acc_sHash = acc_sHash;
        this.acc_bMustChange = acc_bMustChange;
        this.acc_sLockedUntil = "";
        this.acc_sLastSignIn = "";
    }

    public String passwordHash() { return acc_sHash; }
    public boolean usesDefaultPassword() { return acc_bMustChange; }
    public boolean isEnabled() { return !acc_bDisabled; }
    void setEnabled(boolean enabled) { this.acc_bDisabled = !enabled; }
    public int failedAttempts() { return acc_iFails; }
    public String lockedUntil() { return acc_sLockedUntil; }
    public String lastSignIn() { return acc_sLastSignIn; }

    void replaceHash(String acc_sNew) { this.acc_sHash = acc_sNew; this.acc_bMustChange = false; }
    void resetHash(String acc_sNew) { this.acc_sHash = acc_sNew; this.acc_bMustChange = true; }

    void recordFailure(int acc_iMax, java.time.Instant acc_tNow, java.time.Duration acc_dLock) {
        acc_iFails++;
        if (acc_iFails >= acc_iMax) {
            acc_sLockedUntil = acc_tNow.plus(acc_dLock).toString();
            acc_iFails = 0;
        }
    }

    void unlock() { acc_iFails = 0; acc_sLockedUntil = ""; }

    void recordSuccess(java.time.Instant acc_tNow) {
        acc_iFails = 0;
        acc_sLockedUntil = "";
        acc_sLastSignIn = acc_tNow.toString();
    }

    boolean locked(java.time.Instant acc_tNow) {
        if (acc_sLockedUntil == null || acc_sLockedUntil.isEmpty()) return false;
        return acc_tNow.isBefore(java.time.Instant.parse(acc_sLockedUntil));
    }
}
