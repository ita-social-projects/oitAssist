package com.itasocialacademy.oitassist.security.twofactor;

import java.util.Optional;

/**
 * Abstraction over the underlying TOTP (RFC 6238) implementation.
 *
 * <p>
 * Isolates {@code TwoFactorServiceImpl} from the specific third-party library
 * in use (currently {@code com.warrenstrange:googleauth}) — mirrors how
 * {@code JwtHelper} isolates the rest of the app from the {@code jjwt} library.
 * A library swap, or a version bump that changes a method signature, only
 * touches the adapter implementing this interface.
 * </p>
 */
public interface TotpProvider {
    /**
     * Generates a new random base32-encoded TOTP secret.
     */
    String generateSecret();

    /**
     * Builds the standard {@code otpauth://totp/...} provisioning URI encoding the
     * secret, for rendering into a QR code during enrollment.
     *
     * @implNote implementations must return the raw {@code otpauth://} URI and must
     *           never route the secret through any third-party QR-rendering service
     *           — the secret must never leave this server.
     */
    String buildProvisioningUri(String secret, String accountEmail);

    /**
     * Checks a candidate code against the secret, allowing for the configured
     * clock-drift tolerance.
     *
     * @return the matched time-bucket if the code is valid within tolerance; empty
     *         if no bucket in the tolerance window produces a match. The caller
     *         uses the returned bucket for replay-window protection (see plan
     *         section 2.4b).
     */
    Optional<Long> verify(String secret, String code);
}