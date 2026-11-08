package ph.motorph.sp_secpol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The browser side of a session: one opaque {@code HttpOnly} cookie, and a CSRF token the page must echo back.
 *
 * <h2>Why the cookie is HttpOnly</h2>
 * <p>The token never passes through JavaScript, so a cross-site scripting slip cannot read it out of the page. The
 * server sets it, the browser returns it, and no script can see it.</p>
 *
 * <h2>Why there is still a CSRF token</h2>
 * <p>An HttpOnly cookie is sent by the browser automatically, which is exactly what a cross-site request wants. So
 * every state-changing request must also carry a token that only the page could have read: the double-submit
 * pattern. The token is a second random value, delivered in a readable cookie, and the browser copies it into an
 * {@code X-CSRF-Token} header. A cross-site form cannot read the cookie, cannot set the header, and is refused.</p>
 *
 * <h2>Attributes</h2>
 * <ul>
 *   <li>{@code HttpOnly} — no JavaScript access to the session token.</li>
 *   <li>{@code SameSite=Strict} — the browser will not attach it to a cross-site request at all, which is the
 *       primary defence; the CSRF token is the belt to that braces.</li>
 *   <li>{@code Path=/} — one cookie for the application.</li>
 *   <li>{@code Secure} — set whenever the request arrived over HTTPS, so a local HTTP demo still works while a real
 *       deployment never sends the cookie in the clear.</li>
 *   <li>{@code Max-Age} — the absolute session limit, so the browser drops the cookie when the server would.</li>
 * </ul>
 *
 * <p>Nothing here is tied to a device, a location or a person: the token is pure randomness and the server stores
 * only its hash (see {@link SessionStore}).</p>
 */
public final class SessionCookie {
    /** Readable by the page, so it can echo the value in a header. Never the session token. */
    public static final String CSRF_COOKIE = "motorph_csrf";
    public static final String CSRF_HEADER = "X-CSRF-Token";
    private static final int CSRF_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionCookie() {}

    /** A fresh CSRF token: 32 random bytes, base64url without padding. */
    public static String newCsrfToken() {
        byte[] bytes = new byte[CSRF_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The session cookie, as a {@code Set-Cookie} value. {@code secure} is true when the request came over HTTPS. */
    public static String sessionCookie(String token, int maxAgeSeconds, boolean secure) {
        return AuthService.COOKIE + "=" + token
            + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAgeSeconds + (secure ? "; Secure" : "");
    }

    /** The CSRF cookie: same lifetime, deliberately readable, and cleared alongside the session on sign-out. */
    public static String csrfCookie(String csrfToken, int maxAgeSeconds, boolean secure) {
        return CSRF_COOKIE + "=" + csrfToken
            + "; Path=/; SameSite=Strict; Max-Age=" + maxAgeSeconds + (secure ? "; Secure" : "");
    }

    /** Removes both cookies. Max-Age=0 is what deletes them; the values are empty on purpose. */
    public static String[] clearCookies(boolean secure) {
        return new String[] {
            AuthService.COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0" + (secure ? "; Secure" : ""),
            CSRF_COOKIE + "=; Path=/; SameSite=Strict; Max-Age=0" + (secure ? "; Secure" : "")
        };
    }

    /** Reads one cookie from the request's {@code Cookie} header, or null. */
    public static String read(String cookieHeader, String name) {
        if (cookieHeader == null || cookieHeader.isEmpty()) return null;
        for (String pair : cookieHeader.split(";")) {
            int equals = pair.indexOf('=');
            if (equals < 0) continue;
            if (pair.substring(0, equals).trim().equals(name)) return pair.substring(equals + 1).trim();
        }
        return null;
    }

    /**
     * True when the request carries a CSRF token that matches the cookie. Compared in constant time so that a
     * response cannot be used to learn the token one byte at a time.
     */
    public static boolean csrfMatches(String cookieToken, String headerToken) {
        if (cookieToken == null || headerToken == null) return false;
        return MessageDigest.isEqual(cookieToken.getBytes(StandardCharsets.UTF_8), headerToken.getBytes(StandardCharsets.UTF_8));
    }
}
