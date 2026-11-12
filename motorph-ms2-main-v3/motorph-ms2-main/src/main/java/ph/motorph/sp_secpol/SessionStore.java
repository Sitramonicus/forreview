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

    
    public void revoke(String token) {
        if (token == null) return;
        byTokenHash.remove(hash(token));
    }

    
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

    
    public long idleSecondsLeft(String token) {
        SessionRecord record = byTokenHash.get(hash(token));
        if (record == null) return 0;
        long seconds = Duration.between(clock.instant(), record.lastActive().plusSeconds(idleMinutes * 60L)).getSeconds();
        return Math.max(0, seconds);
    }

    
    public int sweep() {
        Instant now = clock.instant();
        int before = byTokenHash.size();
        prune(now);
        return before - byTokenHash.size();
    }

    public void revokeAll() { byTokenHash.clear(); }

    public int size() { return byTokenHash.size(); }

    private void prune(Instant now) {
        byTokenHash.values().removeIf(record -> record.expiredReason(now) != null);
    }

    
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
