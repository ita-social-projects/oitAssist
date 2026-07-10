package com.itasocialacademy.oitassist.participation.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UnableToProcessApplicationException extends BusinessException {
    public UnableToProcessApplicationException(String message) {
        super(message, ErrorCode.REQUEST_CANNOT_BE_PROCESSED);
    }
}
