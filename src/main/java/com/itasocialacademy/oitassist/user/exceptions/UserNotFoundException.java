package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(Long id) {
        super("User with id " + id + " not found", ErrorCode.USER_NOT_FOUND);
    }
}