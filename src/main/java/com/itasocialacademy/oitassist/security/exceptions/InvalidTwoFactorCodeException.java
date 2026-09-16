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
 */
public class InvalidTwoFactorCodeException extends AuthenticationException {
    public InvalidTwoFactorCodeException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}