package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class QuestionVersionConflictException extends BusinessException {
    public QuestionVersionConflictException(Long questionId) {
        super(
            "Question %s was modified by another request".formatted(questionId),
            ErrorCode.QUESTION_VERSION_CONFLICT);
    }
}