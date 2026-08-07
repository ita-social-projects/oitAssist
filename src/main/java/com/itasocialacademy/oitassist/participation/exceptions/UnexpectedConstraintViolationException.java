package com.itasocialacademy.oitassist.participation.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

/**
 * Thrown when a persistence operation fails due to an unexpected database
 * constraint violation that the calling code did not anticipate or cannot
 * recover from (e.g. a constraint other than the one it explicitly checked
 * for).
 *
 * @see TechnicalException
 */

public class UnexpectedConstraintViolationException extends TechnicalException {
    public UnexpectedConstraintViolationException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, errorCode);
        initCause(cause);
    }
}
