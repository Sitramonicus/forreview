package ph.motorph.sp_secpol;

import java.util.Locale;

public final class PasswordPolicy {
    
    public static final String pw_sInitial = "MotorPH-demo-2026";

    
    public static final String pw_sPepperVar = "MOTORPH_PEPPER";

    public static final int pw_iMinLen = 12;

    private PasswordPolicy() {}

    
    public static java.util.List<String> problems(String pw_sValue) {
        java.util.List<String> pw_lProblems = new java.util.ArrayList<>();
        String pw_sClean = pw_sValue == null ? "" : pw_sValue;
        if (pw_sClean.length() < pw_iMinLen) pw_lProblems.add("Use at least " + pw_iMinLen + " characters.");
        if (pw_sClean.equalsIgnoreCase(pw_sInitial)) pw_lProblems.add("Choose a password other than the initial one.");
        boolean pw_bLetter = false, pw_bDigit = false, pw_bSymbol = false;
        for (char pw_c : pw_sClean.toCharArray()) {
            if (Character.isLetter(pw_c)) pw_bLetter = true;
            else if (Character.isDigit(pw_c)) pw_bDigit = true;
            else if (!Character.isWhitespace(pw_c)) pw_bSymbol = true;
        }
        if (!pw_bLetter) pw_lProblems.add("Include at least one letter.");
        if (!pw_bDigit) pw_lProblems.add("Include at least one digit.");
        if (!pw_bSymbol) pw_lProblems.add("Include at least one symbol.");
        if (pw_sClean.toLowerCase(Locale.ROOT).contains("motorph")) pw_lProblems.add("Do not use the company name in the password.");
        return pw_lProblems;
    }

    public static void requireAcceptable(String pw_sValue) {
        java.util.List<String> pw_lProblems = problems(pw_sValue);
        if (!pw_lProblems.isEmpty()) throw new IllegalArgumentException(String.join(" ", pw_lProblems));
    }
}
