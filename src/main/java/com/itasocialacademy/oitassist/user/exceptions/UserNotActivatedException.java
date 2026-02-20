package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserNotActivatedException extends BusinessException {
    public UserNotActivatedException() {
        super("User not activated", ErrorCode.USER_NOT_ACTIVATED);
    }
}
