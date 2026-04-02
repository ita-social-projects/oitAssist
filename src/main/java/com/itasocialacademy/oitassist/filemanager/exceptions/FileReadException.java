package com.itasocialacademy.oitassist.filemanager.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class FileReadException extends TechnicalException {
    public FileReadException(Throwable cause) {
        super("Failed to read uploaded file", ErrorCode.FILE_READ_FAILED, cause);
    }
}
