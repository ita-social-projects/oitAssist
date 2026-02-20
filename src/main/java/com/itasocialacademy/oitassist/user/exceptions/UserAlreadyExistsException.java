package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserAlreadyExistsException extends BusinessException {
    public UserAlreadyExistsException() {
        super("User already exists", ErrorCode.USER_ALREADY_EXISTS);
    }
}
