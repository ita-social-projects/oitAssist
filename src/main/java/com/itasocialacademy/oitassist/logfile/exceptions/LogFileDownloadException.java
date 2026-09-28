package com.itasocialacademy.oitassist.logfile.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class LogFileDownloadException extends TechnicalException {
    public LogFileDownloadException() {
        super("Failed to download log file", ErrorCode.LOG_FILE_DOWNLOAD_FAILED);
    }

    public LogFileDownloadException(Throwable cause) {
        super("Failed to download log file", ErrorCode.LOG_FILE_DOWNLOAD_FAILED, cause);
    }
}
