package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

public class UserContextNotFoundException extends AuthenticationException {
    public UserContextNotFoundException(String message) {
        super(message, ErrorCode.USER_CONTEXT_MISSING);
    }
}
