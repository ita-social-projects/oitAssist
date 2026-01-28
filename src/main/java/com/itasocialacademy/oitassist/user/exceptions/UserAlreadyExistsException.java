package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserAlreadyExistsException extends BusinessException {
    public UserAlreadyExistsException(String email) {
        super("User with email " + email + " already exists", ErrorCode.USER_ALREADY_EXISTS);
    }
}
