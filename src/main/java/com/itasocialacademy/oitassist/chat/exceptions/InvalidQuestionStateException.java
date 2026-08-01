package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class InvalidQuestionStateException extends BusinessException {
    public InvalidQuestionStateException(
        Long questionId,
        QuestionStatus currentStatus,
        String operation) {
        super(
            "Question %s cannot perform operation '%s' while its status is %s"
                .formatted(
                    questionId,
                    operation,
                    currentStatus),
            ErrorCode.QUESTION_INVALID_STATE);
    }

    public InvalidQuestionStateException(
        Long questionId,
        QuestionState currentState,
        String operation) {
        super(
            "Question %s cannot perform operation '%s' while its state is %s"
                .formatted(
                    questionId,
                    operation,
                    currentState),
            ErrorCode.QUESTION_INVALID_STATE);
    }
}