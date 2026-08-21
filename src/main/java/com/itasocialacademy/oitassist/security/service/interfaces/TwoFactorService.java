package com.itasocialacademy.oitassist.security.service.interfaces;

import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorConfirmRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorEnrollRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorEnrollResponse;

/**
 * Enrollment and confirmation for two-factor authentication.
 *
 * <p>
 * Deliberately scoped to enrollment only at this stage (plan sequencing step 3)
 * — login-time verification ({@code /2fa/verify}, consuming the pending token
 * from {@code TokenServiceImpl}) is a later, separate piece once the JWT wiring
 * is connected.
 * </p>
 */
public interface TwoFactorService {
    /**
     * Starts a new enrollment for the given method. Generates a TOTP secret and
     * provisioning URI (for {@code TOTP}) or a pending email code (for
     * {@code EMAIL_OTP}), plus a fresh batch of ten recovery codes. Persists a
     * {@code UserTwoFactorAuth} row with {@code enabled = false} — nothing is
     * actually protected by 2FA until {@link #confirmEnrollment} succeeds.
     *
     * <p>
     * If an enabled enrollment already exists for this user, throws
     * {@code TwoFactorAlreadyEnabledException}. If an unconfirmed prior attempt
     * exists, it's discarded and replaced — nothing was ever protected by it, so
     * there's nothing unsafe about overwriting it.
     * </p>
     *
     * @param userId    the enrolling user's id
     * @param userEmail the enrolling user's email — used as the QR code account
     *                  label and as the email-OTP send target
     * @param request   which method to enroll
     * @return the QR/secret (TOTP) or a signal that a code was emailed (EMAIL_OTP),
     *         plus the ten plaintext recovery codes — shown here once, never
     *         retrievable again
     */
    TwoFactorEnrollResponse enroll(Long userId, String userEmail, TwoFactorEnrollRequest request);

    /**
     * Confirms a pending enrollment by validating a code produced by the
     * just-configured method. Only on success does {@code enabled} flip to
     * {@code true} — this proves the user actually has a working setup before 2FA
     * starts being enforced on their account.
     *
     * @param userId  the user confirming enrollment
     * @param request the code to validate
     * @throws com.itasocialacademy.oitassist.security.exceptions.TwoFactorEnrollmentNotFoundException if
     *                                                                                                 no
     *                                                                                                 enrollment
     *                                                                                                 is
     *                                                                                                 in
     *                                                                                                 progress
     *                                                                                                 for
     *                                                                                                 this
     *                                                                                                 user
     * @throws com.itasocialacademy.oitassist.security.exceptions.InvalidTwoFactorCodeException        if
     *                                                                                                 the
     *                                                                                                 code
     *                                                                                                 does
     *                                                                                                 not
     *                                                                                                 validate
     */
    void confirmEnrollment(Long userId, TwoFactorConfirmRequest request);
}