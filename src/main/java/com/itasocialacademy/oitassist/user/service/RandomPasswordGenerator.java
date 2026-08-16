package com.itasocialacademy.oitassist.user.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Generates cryptographically random strings used as placeholder passwords for
 * users provisioned through external identity providers (OAuth2 / OIDC).
 *
 * <p>
 * Such users authenticate via their provider; they never use this value to log
 * in. The string exists only to satisfy the {@code NOT NULL} constraint on
 * {@code users.password} and to keep the column shape uniform across all
 * accounts. The output is fed through the application's
 * {@link org.springframework.security.crypto.password.PasswordEncoder} before
 * persistence — this class produces the plaintext input only.
 * </p>
 *
 * <p>
 * Encoded as URL-safe Base64 without padding; 32 source bytes yield 43
 * characters, comfortably inside the column's {@code VARCHAR(255)} limit even
 * after bcrypt encoding.
 * </p>
 */
@Component
public class RandomPasswordGenerator {
    private static final int RANDOM_BYTE_COUNT = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Produces a fresh unguessable string. Each invocation returns a distinct
     * value.
     *
     * @return the generated plaintext, ready to be passed to a
     *         {@code PasswordEncoder}
     */
    public String generate() {
        byte[] bytes = new byte[RANDOM_BYTE_COUNT];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
