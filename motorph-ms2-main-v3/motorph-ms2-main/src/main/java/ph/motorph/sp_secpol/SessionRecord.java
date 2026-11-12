package ph.motorph.sp_secpol;

import java.time.Instant;

public final class SessionRecord {
    public final String tokenHash;
    public final String employeeId;
    public final Instant created;
    public final Instant expiresAt;
    
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

    
    
    public String expiredReason(Instant now) {
        if (revoked) return "revoked";
        if (!now.isBefore(expiresAt)) return "absolute";
        if (!now.isBefore(lastActive.plusSeconds(idleMinutes * 60L))) return "idle";
        return null;
    }

    
    boolean touch(Instant now) {
        if (now.isBefore(lastActive.plusSeconds(60))) return false;
        lastActive = now;
        return true;
    }

    void revoke() { revoked = true; }
}
