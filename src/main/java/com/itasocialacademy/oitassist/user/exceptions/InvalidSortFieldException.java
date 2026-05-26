package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class InvalidSortFieldException extends BusinessException {
    public InvalidSortFieldException(String field) {
        super("Sorting by '%s' is not allowed".formatted(field), ErrorCode.COMMON_VALIDATION_FAILED);
    }
}
