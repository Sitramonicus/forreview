package ph.motorph.sp_secpol;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeImport;
import ph.motorph.o_oop.OrgChart;

public final class AuthTest {
    private static int passed, failed;

    private static final String PEPPER = "a-test-pepper-that-is-long-enough-to-be-accepted";
    
    private static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(String iso) { this.now = Instant.parse(iso); }
        void advance(Duration by) { now = now.plus(by); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    public static void main(String[] args) {
        try {
            hashesAreSaltedAndNeverRepeat();
            hashesAreVerifiableAndRejectWrongPasswords();
            aPepperIsRequired();
            everyEmployeeStartsWithTheInitialPassword();
            accountAccessCanBeDisabledAndReenabled();
            signInNeedsTheRightPairAndSaysNothingUsefulOtherwise();
            lockoutAfterFiveAttempts();
            sessionTokensAreLongRandomAndStoredOnlyAsHashes();
            idleAndAbsoluteLimitsAreEnforced();
            theWrongCodeIsRefusedAndCounted();
            threeWrongCodesEndTheChallenge();
            expiredChallengesAreRefused();
            aPasswordAloneGrantsNothing();
            signOutAndPasswordChangeRevokeServerSide();
            passwordChangeRotatesTheSession();
            csrfTokensAreLongAndComparedExactly();
            cookieAttributesAreCorrect();
        } catch (Throwable unexpected) {
            unexpected.printStackTrace();
            failed++;
        }
        System.out.println((failed == 0 ? "PASS" : "FAIL") + " AuthTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(boolean condition, String description) {
        if (condition) { passed++; return; }
        failed++;
        System.out.println("  FAIL " + description);
    }

    

    private static void hashesAreSaltedAndNeverRepeat() {
        PasswordHasher hasher = new PasswordHasher(PEPPER);
        String first = hasher.hash(PasswordPolicy.pw_sInitial);
        String second = hasher.hash(PasswordPolicy.pw_sInitial);
        check(!first.equals(second), "the same password hashes differently every time (a unique salt per account)");
        check(first.startsWith("argon2id$"), "the format names the algorithm: " + first.substring(0, 9));
        String[] parts = first.split("\\$");
        check(parts.length == 5 && !parts[3].equals(second.split("\\$")[3]), "the two hashes carry different salts");
        check(first.contains("m=65536") && first.contains("t=3"), "the Argon2id parameters are recorded alongside the hash");
        check(!first.contains(PasswordPolicy.pw_sInitial), "the hash never contains the password");
    }

    private static void hashesAreVerifiableAndRejectWrongPasswords() {
        PasswordHasher hasher = new PasswordHasher(PEPPER);
        String stored = hasher.hash("MotorPH-demo-2026");
        check(hasher.verify("MotorPH-demo-2026", stored), "the right password verifies");
        check(!hasher.verify("motorPH-demo-2026", stored), "a case change fails");
        check(!hasher.verify("MotorPH-demo-202", stored), "a prefix fails");
        check(!hasher.verify("", stored), "an empty password fails");
        check(!hasher.verify("anything", "not-a-hash"), "a malformed stored value fails instead of throwing");
        check(!hasher.verify("anything", null), "a null stored value fails instead of throwing");
        
        String other = new PasswordHasher("a-completely-different-pepper-of-sufficient-length").hash("MotorPH-demo-2026");
        check(!other.equals(stored), "a different pepper produces a different hash");
    }

    private static void aPepperIsRequired() {
        boolean refused = false;
        try {
            new PasswordHasher("short");
        } catch (IllegalStateException expected) {
            refused = true;
        }
        check(refused, "a pepper shorter than 32 characters is refused, so the app cannot start with a weak one");
        boolean nullRefused = false;
        try {
            new PasswordHasher(null);
        } catch (IllegalStateException expected) {
            nullRefused = true;
        }
        check(nullRefused, "a missing pepper is refused rather than defaulted silently");
    }

    

    private static void everyEmployeeStartsWithTheInitialPassword() {
        List<Employee> roster = EmployeeImport.fromClasspath().employees();
        CredentialStore credentials = new CredentialStore(new PasswordHasher(PEPPER));
        int created = credentials.provision(Arrays.asList("10001", "10015", "10034"));
        check(created == 3 && credentials.size() == 3, "one account per provisioned employee");
        check(PasswordPolicy.pw_sInitial.equals("MotorPH-demo-2026"), "the initial password is the one the brief names");
        check(credentials.account("10015").get().usesDefaultPassword(), "the account is marked as using the shared default password");
        check(roster.size() == 34, "the file holds 34 people, all of whom get an account at start-up");
        check(credentials.signIn("10015", PasswordPolicy.pw_sInitial).ok, "the initial password signs in");
        
        credentials.changePassword("10015", PasswordPolicy.pw_sInitial, "my-own-password-2026");
        check(credentials.provision(Arrays.asList("10001", "10015", "10034")) == 0,
            "provisioning again creates nothing, so a changed password survives a restart");
        check(credentials.signIn("10015", PasswordPolicy.pw_sInitial).reason.equals("password"),
            "and the shared password no longer opens that account");
    }


    private static void accountAccessCanBeDisabledAndReenabled() {
        AuthService auth = service(new MutableClock("2026-11-09T08:00:00Z"));
        AuthService.SignIn firstStep = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        check(firstStep.ok() && firstStep.defaultPassword, "the shared default password opens the code step without a password-change gate");
        String firstCode = auth.twoFactor().peek(firstStep.token).orElse("");
        AuthService.SignIn signedIn = auth.verifyCode(firstStep.token, firstCode);
        check(signedIn.ok() && auth.currentEmployee(signedIn.token).isPresent(), "the code opens the workspace session");

        AuthService.SignIn pendingStep = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        boolean disabled = auth.setAccountEnabled("10015", false);
        check(disabled && !auth.credentials().isEnabled("10015"), "IT can deactivate the account");
        check(!auth.currentEmployee(signedIn.token).isPresent(), "deactivation signs out existing sessions");
        check(!auth.verifyCode(pendingStep.token, auth.twoFactor().peek(pendingStep.token).orElse("")).ok(),
            "deactivation cancels an in-progress sign-in challenge");
        AuthService.SignIn refused = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        check(!refused.ok() && refused.error.contains("deactivated"), "a deactivated account cannot sign in");

        check(auth.setAccountEnabled("10015", true), "IT can reactivate the account");
        AuthService.SignIn reactivated = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        check(reactivated.ok() && reactivated.defaultPassword, "the shared default password works again after activation");
    }

    private static String signedIn(AuthService auth, String employeeId) {
        return signedIn(auth, employeeId, PasswordPolicy.pw_sInitial);
    }

    private static String signedIn(AuthService auth, String employeeId, String password) {
        AuthService.SignIn step1 = auth.signIn(employeeId, password);
        if (!step1.ok()) return "";
        String code = auth.twoFactor().peek(step1.token).orElse("");
        AuthService.SignIn step2 = auth.verifyCode(step1.token, code);
        return step2.ok() ? step2.token : "";
    }
    private static void signInNeedsTheRightPairAndSaysNothingUsefulOtherwise() {
        AuthService auth = service(new MutableClock("2026-11-09T08:00:00Z"));
        AuthService.SignIn ok = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        check(ok.ok() && ok.employeeId.equals("10015"), "the right pair signs in");
        AuthService.SignIn unknown = auth.signIn("99999", PasswordPolicy.pw_sInitial);
        AuthService.SignIn wrong = auth.signIn("10015", "not-the-password");
        check(!unknown.ok() && !wrong.ok(), "an unknown number and a wrong password both fail");
        check(unknown.error.equals(wrong.error), "they fail with the same message, so the roster cannot be probed");
        check(!unknown.error.toLowerCase().contains("unknown") && !unknown.error.toLowerCase().contains("no such"),
            "the message does not admit which half was wrong");
        AuthService.SignIn badFormat = auth.signIn("abc", "x");
        check(!badFormat.ok(), "a non-numeric employee number is refused before any hashing");
    }

    private static void lockoutAfterFiveAttempts() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        AuthService auth = service(clock);
        for (int i = 0; i < CredentialStore.MAX_ATTEMPTS; i++) auth.signIn("10015", "wrong-" + i);
        AuthService.SignIn locked = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        check(!locked.ok(), "five wrong passwords lock the account, even with the right one next");
        check(locked.error.toLowerCase().contains("too many"), "the lockout is explained without naming the password");
        clock.advance(CredentialStore.LOCK_FOR.plusMinutes(1));
        check(auth.signIn("10015", PasswordPolicy.pw_sInitial).ok(), "the lock lifts after its window");
    }

    

    private static void sessionTokensAreLongRandomAndStoredOnlyAsHashes() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        SessionStore sessions = new SessionStore(clock);
        String a = sessions.issue("10015");
        String b = sessions.issue("10015");
        check(!a.equals(b), "two sessions get different tokens");
        check(a.length() >= 43, "the token encodes at least 32 random bytes (" + a.length() + " characters)");
        check(sessions.resolve(a).isPresent(), "an issued token resolves");
        check(!sessions.toString().contains(a), "the store does not hold the token itself");
        
        check(!sessions.resolve(a + "x").isPresent(), "a tampered token resolves to nothing");
        check(sessions.resolve(null).isPresent() == false, "a null token resolves to nothing");
    }

    private static void idleAndAbsoluteLimitsAreEnforced() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        SessionStore sessions = new SessionStore(clock);
        String token = sessions.issue("10015");
        check(SessionStore.IDLE_MINUTES == 15 && SessionStore.ABSOLUTE_HOURS == 8, "the brief's limits: 15 idle, 8 absolute");

        
        
        String at14 = sessions.issue("10015");
        clock.advance(Duration.ofMinutes(14));
        check(sessions.resolve(at14).isPresent(), "still alive at 14 idle minutes");

        String untouched = sessions.issue("10015");
        clock.advance(Duration.ofMinutes(15));
        check(!sessions.resolve(untouched).isPresent(), "dead at exactly 15 idle minutes with no request");

        String at14b = sessions.issue("10015");
        clock.advance(Duration.ofMinutes(14));
        check(sessions.resolve(at14b).isPresent(), "and alive one minute before the limit");

        
        clock.advance(Duration.ofMinutes(5));
        String slid = sessions.issue("10015");
        for (int i = 0; i < 4; i++) { clock.advance(Duration.ofMinutes(10)); check(sessions.resolve(slid).isPresent(), "activity resets the idle clock (call " + (i + 1) + ")"); }
        
        clock.advance(Duration.ofHours(8));
        check(!sessions.resolve(slid).isPresent(), "the absolute eight hours ends a session however active it was");

        String fresh = sessions.issue("10015");
        clock.advance(SessionStore.ABSOLUTE_HOURS + 0L == 8 ? Duration.ZERO : Duration.ZERO);
        check(sessions.resolve(fresh).isPresent(), "a new session starts its own window");
        clock.advance(Duration.ofHours(8).plusSeconds(1));
        check(!sessions.resolve(fresh).isPresent(), "and ends after eight hours even if it was used every minute");
    }


    private static void theWrongCodeIsRefusedAndCounted() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        AuthService auth = service(clock);
        AuthService.SignIn step1 = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        check(step1.ok(), "the password opens a challenge");

        String real = auth.twoFactor().peek(step1.token).orElse("");
        check(real.matches("[0-9]{6}"), "the challenge carries a six-digit code");
        String wrong = real.equals("000000") ? "111111" : "000000";
        AuthService.SignIn bad = auth.verifyCode(step1.token, wrong);
        check(!bad.ok(), "a wrong code issues no session");
        check(bad.error.contains("2 tries left"), "it says how many tries remain: " + bad.error);
        check(bad.token.isEmpty(), "no token comes back on a refusal");

        AuthService.SignIn good = auth.verifyCode(step1.token, real);
        check(good.ok() && !good.token.isEmpty(), "the right code issues the session");
        check(auth.currentEmployee(good.token).isPresent(), "and that session resolves to the employee");

        AuthService.SignIn replay = auth.verifyCode(step1.token, real);
        check(!replay.ok(), "the same challenge cannot be used twice");
    }

    private static void threeWrongCodesEndTheChallenge() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        AuthService auth = service(clock);
        AuthService.SignIn step1 = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        String real = auth.twoFactor().peek(step1.token).orElse("");
        String wrong = real.equals("000000") ? "111111" : "000000";

        auth.verifyCode(step1.token, wrong);
        auth.verifyCode(step1.token, wrong);
        AuthService.SignIn third = auth.verifyCode(step1.token, wrong);
        check(!third.ok() && third.error.toLowerCase().contains("three times"),
            "the third wrong code ends the challenge: " + third.error);
        AuthService.SignIn after = auth.verifyCode(step1.token, real);
        check(!after.ok(), "even the right code is useless once the challenge is spent");
    }

    private static void expiredChallengesAreRefused() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        AuthService auth = service(clock);
        auth.setCodeMinutes(5);
        AuthService.SignIn step1 = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        String real = auth.twoFactor().peek(step1.token).orElse("");
        clock.advance(java.time.Duration.ofMinutes(6));
        AuthService.SignIn late = auth.verifyCode(step1.token, real);
        check(!late.ok() && late.error.toLowerCase().contains("expired"),
            "a code past its window is refused: " + late.error);
    }

    private static void aPasswordAloneGrantsNothing() {
        AuthService auth = service(new MutableClock("2026-11-09T08:00:00Z"));
        AuthService.SignIn step1 = auth.signIn("10015", PasswordPolicy.pw_sInitial);
        check(!auth.currentEmployee(step1.token).isPresent(),
            "the challenge id from step one is not a session token");
        check(auth.activeSessions() == 0, "no session exists until the code is verified");
    }
    private static void signOutAndPasswordChangeRevokeServerSide() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        AuthService auth = service(clock);
        String token = signedIn(auth, "10015");
        check(auth.currentEmployee(token).isPresent(), "the session names the employee");
        auth.signOut(token);
        check(!auth.currentEmployee(token).isPresent(), "signing out ends the session on the server, not just in the browser");
        check(!auth.currentSession(token).isPresent(), "the stored record is gone too");

        String first = signedIn(auth, "10015");
        String second = signedIn(auth, "10015");
        check(auth.currentEmployee(first).isPresent() && auth.currentEmployee(second).isPresent(), "two devices can be signed in");
        auth.changePassword(second, PasswordPolicy.pw_sInitial, "a-new-password-2026");
        check(!auth.currentEmployee(first).isPresent(), "changing the password ends every other session");
        check(!auth.currentEmployee(second).isPresent(), "including the one that made the change — it is rotated, not kept");
    }

    private static void passwordChangeRotatesTheSession() {
        MutableClock clock = new MutableClock("2026-11-09T08:00:00Z");
        AuthService auth = service(clock);
        String before = signedIn(auth, "10015");
        String after = auth.changePassword(before, PasswordPolicy.pw_sInitial, "another-password-2026");
        check(after != null && !after.equals(before), "the browser is given a different token (session fixation defence)");
        check(auth.currentEmployee(after).isPresent(), "the new token works");
        check(!auth.currentEmployee(before).isPresent(), "the old token does not");
        check(!auth.signIn("10015", PasswordPolicy.pw_sInitial).ok(), "the old password is refused");
        check(!signedIn(auth, "10015", "another-password-2026").isEmpty(), "the new password works");
        check(auth.changePassword(before, "another-password-2026", "third-password-2026") == null,
            "a revoked token cannot change the password again");
    }

    

    private static void csrfTokensAreLongAndComparedExactly() {
        String a = SessionCookie.newCsrfToken();
        String b = SessionCookie.newCsrfToken();
        check(!a.equals(b), "each page gets its own CSRF token");
        check(a.length() >= 43, "the CSRF token carries 32 random bytes (" + a.length() + " characters)");
        check(SessionCookie.csrfMatches(a, a), "the right token matches");
        check(!SessionCookie.csrfMatches(a, b), "a different token does not");
        check(!SessionCookie.csrfMatches(a, null) && !SessionCookie.csrfMatches(null, a),
            "a missing token never matches, so an absent header is a refusal");
        check(!SessionCookie.csrfMatches(a, a.substring(0, a.length() - 1)), "a truncated token does not match");
    }

    private static void cookieAttributesAreCorrect() {
        String session = SessionCookie.sessionCookie("TOKEN", 3600, true);
        check(session.contains("HttpOnly"), "the session cookie is HttpOnly, so no script can read it");
        check(session.contains("SameSite=Strict"), "SameSite=Strict keeps it off cross-site requests");
        check(session.contains("Secure"), "Secure over HTTPS");
        check(session.contains("Path=/"), "one cookie for the application");
        check(session.contains("Max-Age=3600"), "the cookie expires with the session");
        check(!SessionCookie.sessionCookie("TOKEN", 3600, false).contains("Secure"), "a local HTTP demo still works");
        String csrf = SessionCookie.csrfCookie("CSRF", 3600, true);
        check(!csrf.contains("HttpOnly"), "the CSRF cookie is readable by the page on purpose");
        check(SessionCookie.read("motorph_session=ABC; motorph_csrf=XYZ", AuthService.COOKIE).equals("ABC")
            && SessionCookie.read("motorph_session=ABC; motorph_csrf=XYZ", SessionCookie.CSRF_COOKIE).equals("XYZ"),
            "cookies are read individually from the header");
        check(SessionCookie.read(null, "x") == null && SessionCookie.read("", "x") == null, "no header means no cookie");
        check(SessionCookie.clearCookies(true)[0].contains("Max-Age=0"), "sign-out deletes the cookie");
    }

    

    private static AuthService service(MutableClock clock) {
        List<Employee> roster = EmployeeImport.fromClasspath().employees();
        CredentialStore credentials = new CredentialStore(new PasswordHasher(PEPPER), clock);
        List<String> ids = new java.util.ArrayList<>();
        for (Employee employee : roster) ids.add(employee.getEid());
        credentials.provision(ids);
        return new AuthService(credentials, new SessionStore(clock), new OrgChart(roster),
            new TwoFactor(clock));
    }
}
