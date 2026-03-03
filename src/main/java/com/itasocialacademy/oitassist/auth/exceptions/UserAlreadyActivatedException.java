package com.itasocialacademy.oitassist.auth.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserAlreadyActivatedException extends BusinessException {
    public UserAlreadyActivatedException() {
        super("User already activated", ErrorCode.USER_ALREADY_ACTIVATED);
    }
}
