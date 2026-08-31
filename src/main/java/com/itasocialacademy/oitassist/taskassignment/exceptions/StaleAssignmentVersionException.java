package com.itasocialacademy.oitassist.taskassignment.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class StaleAssignmentVersionException extends BusinessException {
    public StaleAssignmentVersionException(Long assignmentId) {
        super("Assignment with id %d has been modified by another user.".formatted(assignmentId),
            ErrorCode.ENTITY_VERSION_CONFLICT);
    }
}
