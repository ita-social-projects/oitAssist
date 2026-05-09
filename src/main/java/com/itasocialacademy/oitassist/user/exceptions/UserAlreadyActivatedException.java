package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

/**
 * Thrown when an activation or resend-activation request is made for a user
 * account that is already in {@code ACTIVE} status.
 *
 * <p>
 * Moved from {@code auth.exceptions} to {@code user.exceptions} as part of
 * relocating activation logic into the {@code user} module. Mapped to HTTP 409
 * Conflict via {@code ErrorCode.USER_ALREADY_ACTIVATED}.
 * </p>
 */
public class UserAlreadyActivatedException extends BusinessException {
    public UserAlreadyActivatedException() {
        super("User already activated", ErrorCode.USER_ALREADY_ACTIVATED);
    }
}
