package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserStatusSelfChangeException extends BusinessException {
    public UserStatusSelfChangeException() {
        super("User cannot change their own status", ErrorCode.USER_SELF_CHANGE);
    }
}
