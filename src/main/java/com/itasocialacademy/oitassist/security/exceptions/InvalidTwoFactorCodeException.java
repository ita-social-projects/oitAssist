package com.itasocialacademy.oitassist.security.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;

/**
 * Thrown when a submitted TOTP code, email-OTP code, or recovery code fails
 * verification.
 *
 * <p>
 * Extends {@link AuthenticationException} — a wrong second-factor code is the
 * same category of problem as a wrong password, so it's treated the same way
 * the codebase already treats bad credentials elsewhere (see
 * {@code TokenServiceImpl}'s handling of {@code BadCredentialsException}).
 * </p>
 *
 * <p>
 * <b>Assumption flagged:</b> this extends {@link AuthenticationException}
 * because its (message, {@link ErrorCode}) constructor is the one pattern
 * directly confirmed from real usage in {@code JwtHelper}/{@code JwtFilter}. If
 * the codebase has a more specific base class for this kind of "credential
 * rejected" case, or if {@code ErrorCode} needs a matching new constant added
 * (e.g. {@code INVALID_TWO_FACTOR_CODE}), this should be revisited once those
 * files are available.
 * </p>
 */
public class InvalidTwoFactorCodeException extends AuthenticationException {
    public InvalidTwoFactorCodeException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}