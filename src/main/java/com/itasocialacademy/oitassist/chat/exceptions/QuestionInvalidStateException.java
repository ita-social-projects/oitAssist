package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class QuestionInvalidStateException extends BusinessException {
    public QuestionInvalidStateException(
            Long questionId,
            QuestionState expectedState,
            QuestionState actualState) {

        super(
                "Question with id %s must be in state %s, but is in state %s"
                        .formatted(
                                questionId,
                                expectedState,
                                actualState),
                ErrorCode.QUESTION_INVALID_STATE);
    }
}