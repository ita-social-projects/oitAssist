package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

/**
 * Thrown when an activation token is not found in the database or has passed
 * its expiry time.
 *
 * <p>
 * Moved from {@code auth.exceptions} to {@code user.exceptions} as part of
 * relocating activation logic into the {@code user} module. Mapped to HTTP 400
 * Bad Request via {@code ErrorCode.INVALID_ACTIVATION_TOKEN}.
 * </p>
 */
public class InvalidActivationTokenException extends BusinessException {
    public InvalidActivationTokenException() {
        super("Activation token is invalid or has expired", ErrorCode.INVALID_ACTIVATION_TOKEN);
    }
}
