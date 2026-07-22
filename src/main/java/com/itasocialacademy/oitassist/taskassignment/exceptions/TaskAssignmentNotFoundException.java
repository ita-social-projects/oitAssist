package com.itasocialacademy.oitassist.taskassignment.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class TaskAssignmentNotFoundException extends NotFoundException {
    public TaskAssignmentNotFoundException(Long id) {
        super("Task assignment with id " + id + " was not found",
            ErrorCode.TASK_ASSIGNMENT_NOT_FOUND);
    }
}
