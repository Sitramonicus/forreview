package ph.motorph.web;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Requirement 1 ("login with valid credentials") and the System Admin's security settings for the local demo accounts:
 * a password per account (stored only as a salted PBKDF2 hash), lockout after too many wrong passwords, and the
 * admin-set limits (password length, lockout, 2FA code life, session length).
 * Every account starts with the shared demo password DEFAULT_PASSWORD (written in SETUP.md) until its owner changes it
 * or after an admin resets it. Synthetic demo accounts only; never reuse a real password here.
 */
public final class AccountSecurity {
    /** The demo's starting password for every account (documented in SETUP.md). */
    public static final String DEFAULT_PASSWORD = "MotorPH-demo-2026";
    // shortcut: 20,000 PBKDF2 rounds keep the tests quick; a production system uses far more (or Argon2/bcrypt).
    private static final int ROUNDS = 20_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Thrown when a password is tried again too soon after wrong ones (progressive throttling). Not counted as a wrong try. */
    public static final class TooSoon extends RuntimeException {
        TooSoon(String message) { super(message); }
    }

    public static final class Settings {
        public int minPasswordLength = 8, maxFailedAttempts = 5, lockoutMinutes = 15, codeMinutes = 5, sessionHours = 8;
        public String updatedBy = "", updatedAt = "";
    }

    private static final class Stored {
        String salt, hash, changedAt;
    }

    private Settings settings = new Settings();
    private final Map<String, Stored> passwords = new HashMap<>();
    private final Map<String, Integer> failures = new HashMap<>();
    private final Map<String, String> lockedUntil = new HashMap<>();
    // Real time (seconds matter here); not saved, so a restart clears the waits but not the lockouts.
    private transient Map<String, java.time.Instant> nextTryAt = new HashMap<>();

    /** Seconds to wait after this many wrong passwords in a row: none after the 1st, then 1, 2, 4, 8 ... (design doc: progressive throttling). */
    static long waitSeconds(int failedInARow) {
        return failedInARow < 2 ? 0 : 1L << Math.min(failedInARow - 2, 6);
    }

    public synchronized Settings settings() {
        Settings copy = new Settings();
        copy.minPasswordLength = settings.minPasswordLength;
        copy.maxFailedAttempts = settings.maxFailedAttempts;
        copy.lockoutMinutes = settings.lockoutMinutes;
        copy.codeMinutes = settings.codeMinutes;
        copy.sessionHours = settings.sessionHours;
        copy.updatedBy = settings.updatedBy;
        copy.updatedAt = settings.updatedAt;
        return copy;
    }

    public synchronized void updateSettings(int minLength, int maxFailed, int lockoutMinutes, int codeMinutes, int sessionHours, String by, String at) {
        range(minLength, 8, 64, "Minimum password length");
        range(maxFailed, 3, 10, "Wrong passwords before lockout");
        range(lockoutMinutes, 5, 120, "Lockout minutes");
        range(codeMinutes, 1, 10, "2FA code minutes");
        range(sessionHours, 1, 24, "Session hours");
        settings.minPasswordLength = minLength;
        settings.maxFailedAttempts = maxFailed;
        settings.lockoutMinutes = lockoutMinutes;
        settings.codeMinutes = codeMinutes;
        settings.sessionHours = sessionHours;
        settings.updatedBy = by;
        settings.updatedAt = at;
    }

    /** Throws IllegalStateException while locked, IllegalArgumentException for a wrong password (and locks after too many). */
    public synchronized void checkPassword(String employeeId, String password, LocalDateTime now) {
        Optional<LocalDateTime> locked = lockedUntil(employeeId, now);
        if (locked.isPresent()) throw new IllegalStateException("This account is locked until " + locked.get().toLocalTime() + " after too many wrong passwords.");
        if (nextTryAt == null) nextTryAt = new HashMap<>();
        java.time.Instant allowed = nextTryAt.get(employeeId);
        if (allowed != null && java.time.Instant.now().isBefore(allowed)) {
            long seconds = Math.max(1, java.time.Duration.between(java.time.Instant.now(), allowed).getSeconds() + 1);
            throw new TooSoon("Too many wrong passwords in a row; wait " + seconds + " second" + (seconds == 1 ? "" : "s") + " before trying again.");
        }
        if (matches(employeeId, password)) {
            failures.remove(employeeId);
            nextTryAt.remove(employeeId);
            return;
        }
        int failed = failures.merge(employeeId, 1, Integer::sum);
        long wait = waitSeconds(failed);
        if (wait > 0) nextTryAt.put(employeeId, java.time.Instant.now().plusSeconds(wait));
        else nextTryAt.remove(employeeId);
        if (failed >= settings.maxFailedAttempts) {
            failures.remove(employeeId);
            nextTryAt.remove(employeeId);
            lockedUntil.put(employeeId, now.plusMinutes(settings.lockoutMinutes).toString());
            throw new IllegalStateException("Wrong password " + failed + " times: the account is locked for " + settings.lockoutMinutes + " minutes.");
        }
        int left = settings.maxFailedAttempts - failed;
        throw new IllegalArgumentException("Wrong password; " + left + " tr" + (left == 1 ? "y" : "ies") + " left before the account locks"
            + (wait > 0 ? ", and wait " + wait + " second" + (wait == 1 ? "" : "s") + " before the next try." : "."));
    }

    public synchronized void changePassword(String employeeId, String current, String next, LocalDateTime now) {
        checkPassword(employeeId, current, now);
        String value = next == null ? "" : next;
        if (value.length() < settings.minPasswordLength || value.length() > 64) {
            throw new IllegalArgumentException("The new password must be " + settings.minPasswordLength + " to 64 characters.");
        }
        if (value.equals(current)) throw new IllegalArgumentException("Choose a password different from the current one.");
        if (value.equals(DEFAULT_PASSWORD)) throw new IllegalArgumentException("Choose a password other than the shared demo password.");
        Stored stored = new Stored();
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        stored.salt = hex(salt);
        stored.hash = hex(hash(value, salt));
        stored.changedAt = now.toString();
        passwords.put(employeeId, stored);
    }

    /** Admin: back to the shared demo password, and unlocked. */
    public synchronized void resetPassword(String employeeId) {
        passwords.remove(employeeId);
        unlock(employeeId);
    }

    public synchronized void unlock(String employeeId) {
        failures.remove(employeeId);
        lockedUntil.remove(employeeId);
        if (nextTryAt != null) nextTryAt.remove(employeeId);
    }

    public synchronized boolean usesDefaultPassword(String employeeId) { return !passwords.containsKey(employeeId); }

    public synchronized Optional<String> passwordChangedAt(String employeeId) {
        return Optional.ofNullable(passwords.get(employeeId)).map(stored -> stored.changedAt);
    }

    public synchronized Optional<LocalDateTime> lockedUntil(String employeeId, LocalDateTime now) {
        String until = lockedUntil.get(employeeId);
        if (until == null) return Optional.empty();
        LocalDateTime time = LocalDateTime.parse(until);
        if (!now.isBefore(time)) {
            lockedUntil.remove(employeeId);
            return Optional.empty();
        }
        return Optional.of(time);
    }

    public synchronized int failedAttempts(String employeeId) { return failures.getOrDefault(employeeId, 0); }

    private boolean matches(String employeeId, String password) {
        String given = password == null ? "" : password;
        Stored stored = passwords.get(employeeId);
        if (stored == null) return MessageDigest.isEqual(given.getBytes(java.nio.charset.StandardCharsets.UTF_8), DEFAULT_PASSWORD.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return MessageDigest.isEqual(hash(given, unhex(stored.salt)), unhex(stored.hash));
    }

    private static byte[] hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ROUNDS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing is unavailable.", ex);
        }
    }

    private static void range(int value, int min, int max, String label) {
        if (value < min || value > max) throw new IllegalArgumentException(label + " must be " + min + " to " + max + ".");
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte b : bytes) text.append(String.format("%02x", b));
        return text.toString();
    }

    private static byte[] unhex(String text) {
        byte[] bytes = new byte[text.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }
}
