package ph.motorph.sp_secpol;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-step sign-in: a correct password opens a challenge, and only the right six-digit code opens a session.
 *
 * The code is shown on screen instead of being texted, because this deployment has no SMS gateway and the
 * accounts are provisioned in bulk. A real deployment swaps {@link #codeFor} for an SMS or TOTP sender and
 * nothing else about this class changes.
 *
 * Challenges live for the number of minutes the System Administrator sets, allow three wrong codes, and are
 * deleted the moment they are used, so a code never works twice.
 */
public final class TwoFactor {
    static final int MAX_TRIES = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final class Challenge {
        final String employeeId, code;
        final Instant expires;
        int triesLeft = MAX_TRIES;

        Challenge(String employeeId, String code, Instant expires) {
            this.employeeId = employeeId;
            this.code = code;
            this.expires = expires;
        }
    }

    /** What the caller needs to show the code screen: which challenge, and where the code "went". */
    public static final class Started {
        public final String challengeId, code, employeeId;
        public final LocalDateTime expires;

        Started(String challengeId, String code, String employeeId, LocalDateTime expires) {
            this.challengeId = challengeId;
            this.code = code;
            this.employeeId = employeeId;
            this.expires = expires;
        }
    }

    private final Clock clock;
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public TwoFactor(Clock clock) {
        this.clock = clock;
    }

    public Started start(String employeeId, String mobileEnding, int codeMinutes) {
        String id = token(16);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expires = clock.instant().plusSeconds(codeMinutes * 60L);
        challenges.put(id, new Challenge(employeeId, code, expires));
        return new Started(id, code, employeeId,
            LocalDateTime.ofInstant(expires, clock.getZone()));
    }

    /** The employee the code belongs to, or a refusal saying how many tries are left. */
    public String verify(String challengeId, String code) {
        Challenge challenge = challenges.get(challengeId == null ? "" : challengeId);
        if (challenge == null) {
            throw new IllegalStateException("That code request is over. Sign in again for a new code.");
        }
        if (!clock.instant().isBefore(challenge.expires)) {
            challenges.remove(challengeId);
            throw new IllegalStateException("That code expired. Sign in again for a new code.");
        }
        synchronized (challenge) {
            if (!challenge.code.equals(code == null ? "" : code.trim())) {
                challenge.triesLeft--;
                if (challenge.triesLeft <= 0) {
                    challenges.remove(challengeId);
                    throw new IllegalStateException("Wrong code three times. Sign in again for a new code.");
                }
                throw new IllegalArgumentException("Wrong code; " + challenge.triesLeft + " tr"
                    + (challenge.triesLeft == 1 ? "y" : "ies") + " left.");
            }
        }
        challenges.remove(challengeId);
        return challenge.employeeId;
    }

    public Optional<String> pending(String challengeId) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null) return Optional.empty();
        if (!clock.instant().isBefore(challenge.expires)) {
            challenges.remove(challengeId);
            return Optional.empty();
        }
        return Optional.of(challenge.employeeId);
    }

    /**
     * The code for a live challenge. This exists only because this deployment has no SMS gateway; a real one
     * would never hand the code back to the caller.
     */
    public Optional<String> peek(String challengeId) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null || !clock.instant().isBefore(challenge.expires)) return Optional.empty();
        return Optional.of(challenge.code);
    }

    public int revokeFor(String employeeId) {
        if (employeeId == null) return 0;
        int before = challenges.size();
        challenges.entrySet().removeIf(entry -> employeeId.equals(entry.getValue().employeeId));
        return before - challenges.size();
    }

    public int size() { return challenges.size(); }

    public void clear() { challenges.clear(); }

    private static String token(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        StringBuilder text = new StringBuilder();
        for (byte b : value) text.append(String.format("%02x", b));
        return text.toString();
    }
}
