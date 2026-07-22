package com.itasocialacademy.oitassist.taskassignment.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class TaskAlreadyAssignedException extends BusinessException {
    public TaskAlreadyAssignedException(Long taskBodyId, Long tourId) {
        super("Task " + taskBodyId + " is already assigned to tour " + tourId,
            ErrorCode.TASK_ALREADY_ASSIGNED);
    }
}
