package com.itasocialacademy.oitassist.filemanager.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class FileDeleteException extends TechnicalException {
    public FileDeleteException(String message, Throwable cause) {
        super(message, ErrorCode.FILE_DELETE_FAILED, cause);
    }
}
