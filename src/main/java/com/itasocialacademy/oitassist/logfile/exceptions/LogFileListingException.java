package com.itasocialacademy.oitassist.logfile.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class LogFileListingException extends TechnicalException {
    private static final String DEFAULT_MESSAGE = "Unable to list application log files";

    public LogFileListingException() {
        super(DEFAULT_MESSAGE,
            ErrorCode.LOG_FILE_LISTING_FAILED);
    }
}
