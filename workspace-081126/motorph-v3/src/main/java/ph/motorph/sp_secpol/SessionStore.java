package ph.motorph.sp_secpol;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side session table with lazy, request-driven expiry.
 *
 * The brief's three requirements, and how they are met without a background loop:
 * <ol>
 *   <li><b>Opaque token.</b> 32 bytes from {@link SecureRandom}, base64url encoded. It contains no employee number,
 *       timestamp or expiry — nothing a holder could decode or forge.</li>
 *   <li><b>Stored hashed.</b> Only SHA-256 of the token is kept. The browser holds the raw token in an HttpOnly
 *       cookie; the server cannot leak what it does not have.</li>
 *   <li><b>Expiry checked on every protected request</b> ({@link #resolve}), never on a timer. Expired rows are
 *       pruned on write and by {@link #sweep}, which a caller may run daily; correctness does not depend on it.</li>
 * </ol>
 *
 * Sessions are also invalidated by password changes ({@link #revokeAllFor}) so a changed password cannot leave an
 * older browser signed in.
 */
public final class SessionStore {
    public static final int IDLE_MINUTES = 15;
    public static final int ABSOLUTE_HOURS = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, SessionRecord> byTokenHash = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
    private final Clock clock;
    private final int idleMinutes, absoluteHours;

    public SessionStore() { this(Clock.systemDefaultZone(), IDLE_MINUTES, ABSOLUTE_HOURS); }
    public SessionStore(Clock clock) { this(clock, IDLE_MINUTES, ABSOLUTE_HOURS); }
    public SessionStore(Clock clock, int idleMinutes, int absoluteHours) {
        this.clock = clock;
        this.idleMinutes = idleMinutes;
        this.absoluteHours = absoluteHours;
    }

    public int idleMinutes() { return idleMinutes; }
    public int absoluteHours() { return absoluteHours; }

    /** Issues a session and returns the raw token — the only moment it exists outside the browser. */
    public String issue(String employeeId) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant now = clock.instant();
        byTokenHash.put(hash(token), new SessionRecord(hash(token), employeeId, now,
            now.plus(Duration.ofHours(absoluteHours)), idleMinutes));
        prune(now);
        return token;
    }

    /**
     * Resolves a token to its owner, or empty when the token is unknown, revoked, idle-expired or past its hard
     * deadline. A successful resolve counts as activity.
     */
    public Optional<SessionRecord> resolve(String token) {
        if (token == null || token.isEmpty()) return Optional.empty();
        SessionRecord record = byTokenHash.get(hash(token));
        if (record == null) return Optional.empty();
        Instant now = clock.instant();
        String expired = record.expiredReason(now);
        if (expired != null) {
            byTokenHash.remove(record.tokenHash);
            return Optional.empty();
        }
        record.touch(now);
        return Optional.of(record);
    }

    /** Sign-out: the session stops working immediately, not when the cookie happens to be dropped. */
    public void revoke(String token) {
        if (token == null) return;
        byTokenHash.remove(hash(token));
    }

    /** Password change: every other session for the account dies with it. */
    public int revokeAllFor(String employeeId) {
        int removed = 0;
        List<String> keys = new ArrayList<>(byTokenHash.keySet());
        for (String key : keys) {
            SessionRecord record = byTokenHash.get(key);
            if (record != null && record.employeeId.equals(employeeId)) {
                byTokenHash.remove(key);
                removed++;
            }
        }
        return removed;
    }

    /** Remaining seconds on the idle clock, for the browser's countdown. */
    public long idleSecondsLeft(String token) {
        SessionRecord record = byTokenHash.get(hash(token));
        if (record == null) return 0;
        long seconds = Duration.between(clock.instant(), record.lastActive().plusSeconds(idleMinutes * 60L)).getSeconds();
        return Math.max(0, seconds);
    }

    /** Housekeeping only; expiry is already enforced by {@link #resolve}. */
    public int sweep() {
        Instant now = clock.instant();
        int before = byTokenHash.size();
        prune(now);
        return before - byTokenHash.size();
    }

    public int size() { return byTokenHash.size(); }

    private void prune(Instant now) {
        byTokenHash.values().removeIf(record -> record.expiredReason(now) != null);
    }

    /** SHA-256 of the token, hex. Storing the hash means a database dump cannot be replayed as a login. */
    static String hash(String token) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
