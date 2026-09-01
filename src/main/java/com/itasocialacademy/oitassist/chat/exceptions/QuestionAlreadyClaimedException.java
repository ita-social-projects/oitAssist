package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class QuestionAlreadyClaimedException extends BusinessException {
    public QuestionAlreadyClaimedException(Long questionId) {
        super(
            "Question %s is already claimed for review".formatted(questionId),
            ErrorCode.QUESTION_ALREADY_CLAIMED);
    }
}