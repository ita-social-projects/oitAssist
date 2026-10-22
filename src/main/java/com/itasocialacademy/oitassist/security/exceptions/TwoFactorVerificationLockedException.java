package com.itasocialacademy.oitassist.security.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;

public class TwoFactorVerificationLockedException extends AuthenticationException {
    public TwoFactorVerificationLockedException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
