package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class QuestionNotFoundException extends NotFoundException {
    public QuestionNotFoundException(Long questionId) {
        super(
            "Question with id %s was not found".formatted(questionId),
            ErrorCode.QUESTION_NOT_FOUND);
    }
}