package com.itasocialacademy.oitassist.submission.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class SubmissionNotFoundException extends NotFoundException {
    public SubmissionNotFoundException(Long taskId) {
        super(
            "Submission with id %s was not found".formatted(taskId),
            ErrorCode.SUBMISSION_NOT_FOUND);
    }

    public SubmissionNotFoundException(Long submittedBy, Long taskAssignmentId) {
        super(
            "Submission with submittedBy %s and taskAssignmentId %s was not found".formatted(submittedBy,
                taskAssignmentId),
            ErrorCode.SUBMISSION_NOT_FOUND);
    }
}