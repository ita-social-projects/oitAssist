package com.itasocialacademy.oitassist.security.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;

public class AlgorithmNotFoundException extends TechnicalException {
    public AlgorithmNotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.NO_SUCH_ALGORITHM, cause);
    }
}
