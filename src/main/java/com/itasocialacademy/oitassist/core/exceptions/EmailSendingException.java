package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

public class EmailSendingException extends TechnicalException {
    public EmailSendingException(Throwable cause) {
        super("Failed to send email", ErrorCode.COMMON_INTERNAL_ERROR, cause);
    }
}
