package com.itasocialacademy.oitassist.filemanager.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class FileListingException extends TechnicalException {
    public FileListingException(String message, Throwable cause) {
        super(message, ErrorCode.FILE_LISTING_FAILED, cause);
    }
}
