package ph.motorph.sp_secpol;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    
    public int provision(List<String> employeeIds) {
        int created = 0;
        for (String employeeId : employeeIds) {
            if (accounts.containsKey(employeeId)) continue;
            accounts.put(employeeId, new Account(employeeId, hasher.hash(PasswordPolicy.pw_sInitial), true));
            created++;
        }
        return created;
    }

    public Map<String, Account> exportAccounts() {
        return new LinkedHashMap<>(accounts);
    }

    public void importAccounts(Map<String, Account> saved) {
        if (saved == null) return;
        for (Map.Entry<String, Account> entry : saved.entrySet()) {
            Account account = entry.getValue();
            if (entry.getKey() == null || account == null || !entry.getKey().equals(account.acc_sEid)
                || account.passwordHash() == null || !account.passwordHash().startsWith("argon2id$")) {
                throw new IllegalArgumentException("Invalid stored account");
            }
        }
        accounts.clear();
        accounts.putAll(saved);
    }

    public boolean has(String employeeId) { return employeeId != null && accounts.containsKey(employeeId); }
    public Optional<Account> account(String employeeId) { return Optional.ofNullable(accounts.get(employeeId)); }
    public int size() { return accounts.size(); }

    
    public static final class Result {
        public final boolean ok;
        public final Account account;
        public final String reason;          
        Result(boolean ok, Account account, String reason) { this.ok = ok; this.account = account; this.reason = reason; }
    }

    public Result signIn(String employeeId, String password) {
        Instant now = clock.instant();
        Account account = employeeId == null ? null : accounts.get(employeeId.trim());
        if (account == null) return new Result(false, null, "unknown");
        if (!account.isEnabled()) return new Result(false, account, "inactive");
        if (account.locked(now)) return new Result(false, account, "locked");
        if (!hasher.verify(password, account.passwordHash())) {
            account.recordFailure(MAX_ATTEMPTS, now, LOCK_FOR);
            return new Result(false, account, "password");
        }
        account.recordSuccess(now);
        
        if (hasher.needsRehash(account.passwordHash())) account.replaceHash(hasher.hash(password));
        return new Result(true, account, "");
    }

    
    public boolean changePassword(String employeeId, String currentPassword, String newPassword) {
        Account account = accounts.get(employeeId);
        if (account == null) return false;
        if (!hasher.verify(currentPassword, account.passwordHash())) return false;
        PasswordPolicy.requireAcceptable(newPassword);
        account.replaceHash(hasher.hash(newPassword));
        return true;
    }

    
    public boolean unlock(String employeeId) {
        Account account = accounts.get(employeeId);
        if (account == null) return false;
        account.unlock();
        return true;
    }

    public boolean setEnabled(String employeeId, boolean enabled) {
        Account account = accounts.get(employeeId);
        if (account == null) return false;
        account.setEnabled(enabled);
        if (enabled) account.unlock();
        return true;
    }

    public boolean isEnabled(String employeeId) {
        Account account = accounts.get(employeeId);
        return account != null && account.isEnabled();
    }

    public boolean resetToInitial(String employeeId) {
        Account account = accounts.get(employeeId);
        if (account == null) return false;
        account.resetHash(hasher.hash(PasswordPolicy.pw_sInitial));
        return true;
    }

    
    public List<String> employeeIds() { return new ArrayList<>(accounts.keySet()); }
}
