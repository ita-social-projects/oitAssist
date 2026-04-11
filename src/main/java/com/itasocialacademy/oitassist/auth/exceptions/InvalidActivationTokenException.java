package com.itasocialacademy.oitassist.auth.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class InvalidActivationTokenException extends BusinessException {
    public InvalidActivationTokenException() {
        super("Activation token is invalid or expired", ErrorCode.INVALID_ACTIVATION_TOKEN);
    }
}
