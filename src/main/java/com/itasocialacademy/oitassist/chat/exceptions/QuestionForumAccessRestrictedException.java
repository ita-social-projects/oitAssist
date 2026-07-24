package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;

public class QuestionForumAccessRestrictedException extends AuthorizationException {
    public QuestionForumAccessRestrictedException(Long taskAssignmentId) {
        super(
                "Access to the question forum for task assignment with id %s is restricted"
                        .formatted(taskAssignmentId),
                ErrorCode.QUESTION_ACCESS_RESTRICTED
        );
    }
}