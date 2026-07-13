package com.itasocialacademy.oitassist.task.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class TaskNotFoundException extends NotFoundException {
    public TaskNotFoundException(Long taskId) {
        super(
            "Task with id %s was not found".formatted(taskId),
            ErrorCode.TASK_NOT_FOUND);
    }
}