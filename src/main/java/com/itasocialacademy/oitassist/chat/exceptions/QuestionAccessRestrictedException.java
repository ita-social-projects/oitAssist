package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;

public class QuestionAccessRestrictedException extends AuthorizationException {
    public QuestionAccessRestrictedException(Long questionId) {
        super(
            "Access to question with id %s is restricted".formatted(questionId),
            ErrorCode.QUESTION_ACCESS_RESTRICTED);
    }
}