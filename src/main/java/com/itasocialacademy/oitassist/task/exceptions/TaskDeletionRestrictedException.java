package com.itasocialacademy.oitassist.task.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class TaskDeletionRestrictedException extends BusinessException {
    public TaskDeletionRestrictedException(Long taskId) {
        super("Cannot delete task with id: %s".formatted(taskId),
            ErrorCode.TASK_DELETION_RESTRICTED);
    }
}
