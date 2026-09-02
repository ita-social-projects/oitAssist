package com.itasocialacademy.oitassist.logfile.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class LogFileNotFoundException extends NotFoundException {
    public LogFileNotFoundException(String fileName) {
        super("Log file not found " + fileName, ErrorCode.LOG_FILE_NOT_FOUND);
    }
}
