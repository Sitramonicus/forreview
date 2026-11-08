package ph.motorph.sp_secpol;

import java.time.Instant;

/**
 * One server-side session. The raw token is never stored — only its SHA-256 hash, so a leaked session table yields
 * nothing a browser could present.
 *
 * Two clocks:
 * <ul>
 *   <li>{@code lastActive} + {@code idleTimeout} — a person who walks away is signed out.</li>
 *   <li>{@code expiresAt} — an absolute deadline that activity never extends.</li>
 * </ul>
 */
public final class SessionRecord {
    public final String tokenHash;
    public final String employeeId;
    public final Instant created;
    public final Instant expiresAt;
    /** Reported to the browser for the countdown; the server's own check is what actually decides. */
    public final int idleMinutes;

    private Instant lastActive;
    private boolean revoked;

    public SessionRecord(String tokenHash, String employeeId, Instant created, Instant expiresAt, int idleMinutes) {
        this.tokenHash = tokenHash;
        this.employeeId = employeeId;
        this.created = created;
        this.expiresAt = expiresAt;
        this.idleMinutes = idleMinutes;
        this.lastActive = created;
    }

    public Instant lastActive() { return lastActive; }
    public boolean revoked() { return revoked; }

    /** Why this session is no longer usable, or {@code null} when it still is. */
    public String expiredReason(Instant now) {
        if (revoked) return "revoked";
        if (!now.isBefore(expiresAt)) return "absolute";
        if (now.isAfter(lastActive.plusSeconds(idleMinutes * 60L))) return "idle";
        return null;
    }

    /**
     * Moves the idle clock forward. Writes are throttled to once a minute: the idle limit stays accurate to within
     * a minute, and a busy screen does not cost a write per request.
     */
    boolean touch(Instant now) {
        if (now.isBefore(lastActive.plusSeconds(60))) return false;
        lastActive = now;
        return true;
    }

    void revoke() { revoked = true; }
}
