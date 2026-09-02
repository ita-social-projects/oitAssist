package com.itasocialacademy.oitassist.logfile.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;

public class InvalidLogFileNameException extends ValidationException {
    public InvalidLogFileNameException() {
        super("Invalid log file name", ErrorCode.INVALID_LOG_FILE_NAME);
    }
}
