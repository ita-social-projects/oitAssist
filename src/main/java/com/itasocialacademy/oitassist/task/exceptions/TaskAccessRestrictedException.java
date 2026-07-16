package com.itasocialacademy.oitassist.task.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class TaskAccessRestrictedException extends BusinessException {
    public TaskAccessRestrictedException(Long taskId) {
        super(
            "Cannot access task with id: %s".formatted(taskId),
            ErrorCode.TASK_ACCESS_RESTRICTED);
    }
}
