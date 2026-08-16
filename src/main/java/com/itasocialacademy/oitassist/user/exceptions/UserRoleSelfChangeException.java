package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserRoleSelfChangeException extends BusinessException {
    public UserRoleSelfChangeException() {
        super("User cannot change their own role", ErrorCode.USER_SELF_CHANGE);
    }
}
