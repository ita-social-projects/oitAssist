package com.itasocialacademy.oitassist.task.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class TaskNotFoundException extends BusinessException {
    public TaskNotFoundException(Long taskId) {
        super(
            "Task with id %s not found".formatted(taskId),
            ErrorCode.TASK_NOT_FOUND);
    }
}