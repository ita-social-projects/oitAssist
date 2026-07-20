package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;

public class UserAuthorizationException extends AuthorizationException {
    public UserAuthorizationException() {
        super("User is not authenticated", ErrorCode.ACCESS_DENIED);
    }
}
