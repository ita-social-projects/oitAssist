package com.itasocialacademy.oitassist.security.service.interfaces;

import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorConfirmRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorEnrollRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorEnrollResponse;
import com.itasocialacademy.oitassist.security.exceptions.InvalidTwoFactorCodeException;
import com.itasocialacademy.oitassist.security.exceptions.TwoFactorEnrollmentNotFoundException;

/**
 * Enrollment, confirmation, and login-time verification for two-factor
 * authentication.
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

    /**
     * Validates a login-time code against an already-enabled 2FA setup — either the
     * user's configured method (TOTP/email-OTP) or one of their recovery codes,
     * tried as a fallback if the primary method's code doesn't match. On success,
     * mutates and persists whatever state the matched path requires (records the
     * TOTP time-bucket for replay protection, clears a consumed email-OTP, or marks
     * a recovery code used).
     *
     * @param userId the user completing the challenge
     * @param code   the TOTP code, email-OTP code, or recovery code submitted
     * @throws TwoFactorEnrollmentNotFoundException if the user has no enabled 2FA
     *                                              setup (shouldn't normally happen
     *                                              if the caller only reaches this
     *                                              after a
     *                                              {@code TWO_FA_VERIFICATION_REQUIRED}
     *                                              login outcome, but guards
     *                                              against a race where 2FA was
     *                                              disabled in between)
     * @throws InvalidTwoFactorCodeException        if neither the primary method
     *                                              nor any recovery code matches
     */
    void verify(Long userId, String code);

    /**
     * Generates and dispatches a fresh email-OTP code for a user whose 2FA method
     * is {@code EMAIL_OTP}, for use at login time. Unlike TOTP (where the user's
     * own authenticator app generates codes independently, no server action
     * needed), an email-OTP user has nothing to submit at login until the server
     * sends them something — this is that send.
     *
     * @param userId    the user logging in
     * @param userEmail where to send the code
     * @throws com.itasocialacademy.oitassist.security.exceptions.TwoFactorEnrollmentNotFoundException if
     *                                                                                                 the
     *                                                                                                 user
     *                                                                                                 has
     *                                                                                                 no
     *                                                                                                 enabled
     *                                                                                                 2FA
     *                                                                                                 setup
     */
    void resendLoginOtp(Long userId, String userEmail);
}