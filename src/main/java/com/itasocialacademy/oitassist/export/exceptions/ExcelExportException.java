package com.itasocialacademy.oitassist.export.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class ExcelExportException extends TechnicalException {
    public ExcelExportException(String message, Throwable cause) {
        super(message, ErrorCode.COMMON_INTERNAL_ERROR, cause);
    }
}
