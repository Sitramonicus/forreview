package ph.motorph.sp_secpol;

import java.util.Locale;

/**
 * Password rules, the initial provisioning credential, and the pepper's environment variable name.
 *
 * The initial password is deliberately public: it is in the assignment brief and in SETUP.md so that every account
 * can be signed into once. That is exactly why the first sign-in must change it — {@link CredentialStore} marks the
 * account {@code mustChangePassword} until it does.
 */
public final class PasswordPolicy {
    /** The credential every imported account starts with. Case-sensitive. Never stored in plain text. */
    public static final String INITIAL_PASSWORD = "MotorPH-demo-2026";

    /**
     * Where the pepper comes from. It must be an unpredictable value supplied by the deployment (environment
     * variable or secret manager) — never derived from an employee number, name, birthday, email, location or time,
     * because all of those are either guessable or already in the database.
     */
    public static final String PEPPER_VARIABLE = "MOTORPH_PASSWORD_PEPPER";

    public static final int MIN_LENGTH = 12;

    private PasswordPolicy() {}

    /** Reasons the password is unacceptable; empty when it is fine. */
    public static java.util.List<String> problems(String password) {
        java.util.List<String> problems = new java.util.ArrayList<>();
        String value = password == null ? "" : password;
        if (value.length() < MIN_LENGTH) problems.add("Use at least " + MIN_LENGTH + " characters.");
        if (value.equalsIgnoreCase(INITIAL_PASSWORD)) problems.add("Choose a password other than the initial one.");
        boolean hasLetter = false, hasDigit = false, hasSymbol = false;
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isWhitespace(c)) hasSymbol = true;
        }
        if (!hasLetter) problems.add("Include at least one letter.");
        if (!hasDigit) problems.add("Include at least one digit.");
        if (!hasSymbol) problems.add("Include at least one symbol.");
        if (value.toLowerCase(Locale.ROOT).contains("motorph")) problems.add("Do not use the company name in the password.");
        return problems;
    }

    public static void requireAcceptable(String password) {
        java.util.List<String> problems = problems(password);
        if (!problems.isEmpty()) throw new IllegalArgumentException(String.join(" ", problems));
    }
}
