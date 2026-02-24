package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;
import org.springframework.modulith.NamedInterface;

@NamedInterface("UserNotFoundException")
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super("User not found", ErrorCode.USER_NOT_FOUND);
    }
}