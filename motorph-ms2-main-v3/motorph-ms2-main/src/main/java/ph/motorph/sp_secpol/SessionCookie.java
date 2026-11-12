package ph.motorph.sp_secpol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class SessionCookie {
    
    public static final String CSRF_COOKIE = "motorph_csrf";
    public static final String CSRF_HEADER = "X-CSRF-Token";
    private static final int CSRF_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionCookie() {}

    
    public static String newCsrfToken() {
        byte[] bytes = new byte[CSRF_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    
    public static String sessionCookie(String token, int maxAgeSeconds, boolean secure) {
        return AuthService.COOKIE + "=" + token
            + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAgeSeconds + (secure ? "; Secure" : "");
    }

    
    public static String csrfCookie(String csrfToken, int maxAgeSeconds, boolean secure) {
        return CSRF_COOKIE + "=" + csrfToken
            + "; Path=/; SameSite=Strict; Max-Age=" + maxAgeSeconds + (secure ? "; Secure" : "");
    }

    
    public static String[] clearCookies(boolean secure) {
        return new String[] {
            AuthService.COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0" + (secure ? "; Secure" : ""),
            CSRF_COOKIE + "=; Path=/; SameSite=Strict; Max-Age=0" + (secure ? "; Secure" : "")
        };
    }

    
    public static String read(String cookieHeader, String name) {
        if (cookieHeader == null || cookieHeader.isEmpty()) return null;
        for (String pair : cookieHeader.split(";")) {
            int equals = pair.indexOf('=');
            if (equals < 0) continue;
            if (pair.substring(0, equals).trim().equals(name)) return pair.substring(equals + 1).trim();
        }
        return null;
    }

    
    public static boolean csrfMatches(String cookieToken, String headerToken) {
        if (cookieToken == null || headerToken == null) return false;
        return MessageDigest.isEqual(cookieToken.getBytes(StandardCharsets.UTF_8), headerToken.getBytes(StandardCharsets.UTF_8));
    }
}
