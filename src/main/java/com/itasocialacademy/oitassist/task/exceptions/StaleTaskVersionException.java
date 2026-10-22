package com.itasocialacademy.oitassist.task.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class StaleTaskVersionException extends BusinessException {
    public StaleTaskVersionException(Long taskId) {
        super("Task with id %d has been modified by another user. ".formatted(taskId),
            ErrorCode.ENTITY_VERSION_CONFLICT);
    }
}
