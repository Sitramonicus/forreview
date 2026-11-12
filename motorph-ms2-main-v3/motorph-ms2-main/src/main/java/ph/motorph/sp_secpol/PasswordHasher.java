package ph.motorph.sp_secpol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

public final class PasswordHasher {
    private static final int VERSION = 19;              
    private static final int MEMORY_KIB = 64 * 1024;    
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 1;
    private static final int HASH_BYTES = 32;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] pepperBytes;

    public PasswordHasher(String pepper) {
        if (pepper == null || pepper.trim().length() < 32) {
            throw new IllegalStateException(PasswordPolicy.pw_sPepperVar + " must be set to at least 32 characters; "
                + "refusing to start with a weak or missing pepper.");
        }
        this.pepperBytes = pepper.getBytes(StandardCharsets.UTF_8);
    }

    
    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] digest = derive(password, salt, MEMORY_KIB, ITERATIONS, PARALLELISM);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return "argon2id$v=" + VERSION
            + "$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
            + "$" + b64.encodeToString(salt)
            + "$" + b64.encodeToString(digest);
    }

    
    public boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 5 || !"argon2id".equals(parts[0])) return false;
        try {
            int memory = 65536, iterations = 3, parallelism = 1;
            for (String setting : parts[2].split(",")) {
                String[] pair = setting.split("=");
                if (pair.length != 2) continue;
                switch (pair[0]) {
                    case "m": memory = Integer.parseInt(pair[1]); break;
                    case "t": iterations = Integer.parseInt(pair[1]); break;
                    case "p": parallelism = Integer.parseInt(pair[1]); break;
                    default: break;
                }
            }
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] salt = b64.decode(parts[3]);
            byte[] expected = b64.decode(parts[4]);
            byte[] actual = derive(password, salt, memory, iterations, parallelism);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException malformed) {
            return false;
        }
    }

    
    public boolean needsRehash(String stored) {
        return stored == null || !stored.startsWith("argon2id$v=" + VERSION + "$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM + "$");
    }

    private byte[] derive(String password, byte[] salt, int memoryKib, int iterations, int parallelism) {
        
        
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] combinedSalt = new byte[salt.length + pepperBytes.length];
        System.arraycopy(salt, 0, combinedSalt, 0, salt.length);
        System.arraycopy(pepperBytes, 0, combinedSalt, salt.length, pepperBytes.length);
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(VERSION)
            .withMemoryAsKB(memoryKib)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .withSalt(combinedSalt)
            .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] out = new byte[HASH_BYTES];
        generator.generateBytes(passwordBytes, out);
        Arrays.fill(passwordBytes, (byte) 0);
        return out;
    }

}
