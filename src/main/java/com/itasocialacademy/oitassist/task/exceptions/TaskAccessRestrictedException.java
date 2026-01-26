package com.itasocialacademy.oitassist.task.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class TaskAccessRestrictedException extends BusinessException {
    public TaskAccessRestrictedException(Long taskId, Long userId) {
        super(
                "User %s cannot access task %s".formatted(userId, taskId),
                ErrorCode.TASK_ACCESS_RESTRICTED
        );
    }
}
