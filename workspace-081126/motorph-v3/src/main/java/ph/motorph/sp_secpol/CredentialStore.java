package ph.motorph.sp_secpol;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The account table: who can sign in, with what hash, and whether they have changed their first password.
 *
 * Provisioning is done from the employee file, so there is exactly one account per employee number and no account
 * for anyone who is not on the roster. A missing employee is not created on demand — that would let a typo mint an
 * account.
 *
 * Failed sign-ins are throttled per account with a growing lockout. The error the caller sees is identical for
 * "no such employee" and "wrong password", so the form cannot be used to enumerate who works here.
 */
public final class CredentialStore {
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_FOR = Duration.ofMinutes(15);

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final PasswordHasher hasher;
    private final Clock clock;

    public CredentialStore(PasswordHasher hasher) { this(hasher, Clock.systemDefaultZone()); }
    public CredentialStore(PasswordHasher hasher, Clock clock) {
        this.hasher = hasher;
        this.clock = clock;
    }

    /**
     * Creates the initial account for every employee in the roster that does not already have one.
     * Existing accounts are left alone so a restart never resets somebody's password or first-login flag.
     */
    public int provision(List<String> employeeIds) {
        int created = 0;
        for (String employeeId : employeeIds) {
            if (accounts.containsKey(employeeId)) continue;
            accounts.put(employeeId, new Account(employeeId, hasher.hash(PasswordPolicy.INITIAL_PASSWORD), true));
            created++;
        }
        return created;
    }

    public boolean has(String employeeId) { return employeeId != null && accounts.containsKey(employeeId); }
    public Optional<Account> account(String employeeId) { return Optional.ofNullable(accounts.get(employeeId)); }
    public int size() { return accounts.size(); }

    /** Outcome of a sign-in attempt. {@code reason} is for logs, never for the browser. */
    public static final class Result {
        public final boolean ok;
        public final Account account;
        public final String reason;          // "unknown" | "locked" | "password" | "" on success
        Result(boolean ok, Account account, String reason) { this.ok = ok; this.account = account; this.reason = reason; }
    }

    public Result signIn(String employeeId, String password) {
        Instant now = clock.instant();
        Account account = employeeId == null ? null : accounts.get(employeeId.trim());
        if (account == null) return new Result(false, null, "unknown");
        if (account.locked(now)) return new Result(false, account, "locked");
        if (!hasher.verify(password, account.passwordHash())) {
            account.recordFailure(MAX_ATTEMPTS, now, LOCK_FOR);
            return new Result(false, account, "password");
        }
        account.recordSuccess(now);
        // Parameters may have been raised since this hash was written; upgrade it on the way through.
        if (hasher.needsRehash(account.passwordHash())) account.replaceHash(hasher.hash(password));
        return new Result(true, account, "");
    }

    /**
     * Changes a password after verifying the current one. Returns false when the current password is wrong, so the
     * caller can answer 403 without revealing anything else.
     */
    public boolean changePassword(String employeeId, String currentPassword, String newPassword) {
        Account account = accounts.get(employeeId);
        if (account == null) return false;
        if (!hasher.verify(currentPassword, account.passwordHash())) return false;
        PasswordPolicy.requireAcceptable(newPassword);
        account.replaceHash(hasher.hash(newPassword));
        return true;
    }

    /** System Administrator reset: restores the initial credential and re-arms the change-on-next-sign-in flag. */
    public boolean resetToInitial(String employeeId) {
        Account account = accounts.get(employeeId);
        if (account == null) return false;
        account.replaceHash(hasher.hash(PasswordPolicy.INITIAL_PASSWORD));
        return true;
    }

    /** Only used by tests and by the System Administrator's "reset provisioning" action. */
    public List<String> employeeIds() { return new ArrayList<>(accounts.keySet()); }
}
