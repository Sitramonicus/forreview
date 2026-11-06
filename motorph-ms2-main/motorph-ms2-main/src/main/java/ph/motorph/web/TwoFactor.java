package ph.motorph.web;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Requirement 1 / security: simulated two-factor sign-in for the local demo accounts. After the right password the
 * server "texts" a 6-digit code (the demo shows it on screen, because there is no phone); the right code within the
 * admin's code time gives a session token for the admin's session length, and every request as that account must carry it.
 * shortcut: codes are shown instead of sent, and sessions live in memory (a server restart signs everyone out);
 * a real system sends the code by SMS or an authenticator app.
 */
public final class TwoFactor {
    static final int MAX_TRIES = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final class Challenge {
        final String employeeId, code;
        final LocalDateTime expires;
        int triesLeft = MAX_TRIES;

        Challenge(String employeeId, String code, LocalDateTime expires) {
            this.employeeId = employeeId;
            this.code = code;
            this.expires = expires;
        }
    }

    public static final class Started {
        public final String challengeId, code;
        public final LocalDateTime expires;

        Started(String challengeId, String code, LocalDateTime expires) {
            this.challengeId = challengeId;
            this.code = code;
            this.expires = expires;
        }
    }

    private final Supplier<LocalDateTime> now;
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionExpires = new ConcurrentHashMap<>();

    public TwoFactor(Supplier<LocalDateTime> now) {
        this.now = now;
    }

    public Started start(String employeeId, int codeMinutes) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expires = now.get().plusMinutes(codeMinutes);
        String id = token(12);
        challenges.put(id, new Challenge(employeeId, code, expires));
        return new Started(id, code, expires);
    }

    /** The session token, or an exception saying why not (wrong code with tries left, too many tries, expired). */
    public String verify(String challengeId, String code, int sessionHours) {
        Challenge challenge = challenges.get(challengeId == null ? "" : challengeId);
        if (challenge == null) throw new IllegalStateException("That code request is over; choose the account again for a new code.");
        if (now.get().isAfter(challenge.expires)) {
            challenges.remove(challengeId);
            throw new IllegalStateException("That code expired; choose the account again for a new code.");
        }
        synchronized (challenge) {
            if (!challenge.code.equals(code == null ? "" : code.trim())) {
                challenge.triesLeft--;
                if (challenge.triesLeft <= 0) {
                    challenges.remove(challengeId);
                    throw new IllegalStateException("Wrong code 3 times; choose the account again for a new code.");
                }
                throw new IllegalArgumentException("Wrong code; " + challenge.triesLeft + " tr" + (challenge.triesLeft == 1 ? "y" : "ies") + " left.");
            }
        }
        challenges.remove(challengeId);
        String session = token(16);
        sessions.put(session, challenge.employeeId);
        sessionExpires.put(session, now.get().plusHours(sessionHours));
        return session;
    }

    /** The account a session belongs to, while it has not expired (the admin's session length). */
    public Optional<String> sessionOwner(String session) {
        if (session == null || !sessions.containsKey(session.trim())) return Optional.empty();
        String key = session.trim();
        if (now.get().isAfter(sessionExpires.getOrDefault(key, LocalDateTime.MIN))) {
            signOut(key);
            return Optional.empty();
        }
        return Optional.of(sessions.get(key));
    }

    public void signOut(String session) {
        if (session == null) return;
        sessions.remove(session.trim());
        sessionExpires.remove(session.trim());
    }

    /** Ends every session of one account except {@code keep} (after a password change or an admin reset). */
    public void signOutEverywhere(String employeeId, String keep) {
        sessions.entrySet().removeIf(entry -> {
            boolean mine = entry.getValue().equals(employeeId) && !entry.getKey().equals(keep);
            if (mine) sessionExpires.remove(entry.getKey());
            return mine;
        });
    }

    private static String token(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        StringBuilder text = new StringBuilder();
        for (byte b : value) text.append(String.format("%02x", b));
        return text.toString();
    }
}
