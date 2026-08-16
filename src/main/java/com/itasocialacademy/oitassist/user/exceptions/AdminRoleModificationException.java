package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class AdminRoleModificationException extends BusinessException {
    public AdminRoleModificationException() {
        super("Cannot modify role of another administrator", ErrorCode.ADMIN_MODIFICATION_RESTRICTED);
    }
}
