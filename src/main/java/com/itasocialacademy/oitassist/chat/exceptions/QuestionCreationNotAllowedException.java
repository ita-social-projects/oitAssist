package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class QuestionCreationNotAllowedException extends BusinessException {
    public QuestionCreationNotAllowedException(Long taskAssignmentId, ExecutionStatus executionStatus) {
        super(
                "Question creation is not allowed for task assignment with id %s while the tour status is %s"
                        .formatted(taskAssignmentId, executionStatus),
                ErrorCode.QUESTION_INVALID_STATE
        );
    }
}