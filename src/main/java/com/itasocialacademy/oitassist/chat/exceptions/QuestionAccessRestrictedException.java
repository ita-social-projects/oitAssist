package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;

public class QuestionAccessRestrictedException extends AuthorizationException {
    public QuestionAccessRestrictedException(Long questionId) {
        this("Access to question with id %s is restricted".formatted(questionId));
    }

    private QuestionAccessRestrictedException(String message) {
        super(
            message,
            ErrorCode.QUESTION_ACCESS_RESTRICTED);
    }

    public static QuestionAccessRestrictedException forTaskForum(Long taskId) {
        return new QuestionAccessRestrictedException(
            "Access to question forum for task with id %s is restricted".formatted(taskId));
    }
}