package com.itasocialacademy.oitassist.filemanager.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class InvalidFilePathException extends BusinessException {
    public InvalidFilePathException(String message) {
        super(message, ErrorCode.INVALID_FILE_PATH);
    }
}