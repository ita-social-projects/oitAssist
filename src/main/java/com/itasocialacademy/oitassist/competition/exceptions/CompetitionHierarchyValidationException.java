package com.itasocialacademy.oitassist.competition.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class CompetitionHierarchyValidationException extends BusinessException {
    public CompetitionHierarchyValidationException(String message) {
        super(message, ErrorCode.COMMON_VALIDATION_FAILED);
    }
}
