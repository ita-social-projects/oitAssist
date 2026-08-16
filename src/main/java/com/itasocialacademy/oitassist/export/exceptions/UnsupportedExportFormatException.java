package com.itasocialacademy.oitassist.export.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UnsupportedExportFormatException extends BusinessException {
    public UnsupportedExportFormatException(String message) {
        super(message, ErrorCode.COMMON_VALIDATION_FAILED);
    }
}
