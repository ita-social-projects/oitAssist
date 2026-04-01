package com.itasocialacademy.oitassist.filemanager.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class FileUploadException extends TechnicalException {
    public FileUploadException(String filename, Throwable cause) {
        super("Failed to read uploaded file: " + filename, ErrorCode.FILE_UPLOAD_FAILED, cause);
    }
}
