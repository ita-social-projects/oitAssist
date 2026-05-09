package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

/**
 * Thrown during registration when a user attempts to register with an email
 * that already has a {@code PENDING} account — registered but not yet
 * email-verified.
 *
 * <p>
 * Moved from {@code auth.exceptions} to {@code user.exceptions} as part of
 * relocating registration logic into the {@code user} module. Mapped to HTTP
 * 409 Conflict via {@code ErrorCode.USER_NOT_ACTIVATED}.
 * </p>
 */
public class UserNotActivatedException extends BusinessException {
    public UserNotActivatedException() {
        super("User already registered but not yet activated", ErrorCode.USER_NOT_ACTIVATED);
    }
}
