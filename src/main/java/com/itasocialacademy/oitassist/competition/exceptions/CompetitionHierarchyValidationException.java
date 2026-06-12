package com.itasocialacademy.oitassist.competition.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AppException;

public class CompetitionHierarchyValidationException extends AppException {
    public CompetitionHierarchyValidationException(String message) {
        super(message, ErrorCode.COMMON_VALIDATION_FAILED);
    }
}
