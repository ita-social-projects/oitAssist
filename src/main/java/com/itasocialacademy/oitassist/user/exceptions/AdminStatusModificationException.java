package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class AdminStatusModificationException extends BusinessException {
    public AdminStatusModificationException() {
        super("Cannot modify status of another administrator", ErrorCode.ADMIN_MODIFICATION_RESTRICTED);
    }
}
