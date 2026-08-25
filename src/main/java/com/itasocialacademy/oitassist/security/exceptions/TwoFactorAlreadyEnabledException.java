package com.itasocialacademy.oitassist.security.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

/**
 * Thrown when a user who already has 2FA enabled attempts to enroll again
 * without first disabling or going through {@code /2fa/change-method}.
 *
 * <p>
 * Extends {@link BusinessException} (previously assumed
 * {@code AuthenticationException} — corrected). This is a state-conflict, not
 * an authentication failure: the user has proven who they are just fine; the
 * problem is that the action they're requesting doesn't make sense given the
 * current state of their account. Its {@link ErrorCode} constant carries
 * {@code ErrorCategory.CONFLICT}, which maps to HTTP 409 — not 401. A 401 here
 * would incorrectly suggest to a client that re-authentication is needed, when
 * the actual fix is "go to account settings," not "log in again."
 * </p>
 */
public class TwoFactorAlreadyEnabledException extends BusinessException {
    public TwoFactorAlreadyEnabledException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}