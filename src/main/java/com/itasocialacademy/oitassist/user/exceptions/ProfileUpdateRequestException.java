package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class ProfileUpdateRequestException extends BusinessException {
    public ProfileUpdateRequestException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
