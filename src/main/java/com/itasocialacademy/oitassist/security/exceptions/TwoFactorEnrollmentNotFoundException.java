package com.itasocialacademy.oitassist.security.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

/**
 * Thrown when {@code confirmEnrollment}/{@code verify} is called but no
 * {@code UserTwoFactorAuth} row exists for the user — i.e. {@code enroll()} was
 * never called, or a previous attempt was already cleared.
 *
 * <p>
 * Kept distinct from {@link InvalidTwoFactorCodeException} deliberately: "no
 * enrollment in progress" and "wrong code" are different failures a client
 * should react to differently (redirect to start enrollment, vs. let the user
 * retry).
 * </p>
 */
public class TwoFactorEnrollmentNotFoundException extends NotFoundException {
    public TwoFactorEnrollmentNotFoundException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}