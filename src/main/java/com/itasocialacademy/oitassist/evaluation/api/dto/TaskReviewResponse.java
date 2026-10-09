package com.itasocialacademy.oitassist.evaluation.api.dto;

import java.util.List;

/**
 * Everything the jury needs to review one task assignment: the reference
 * solution files and the anonymized submissions.
 */
public record TaskReviewResponse(
    Long taskAssignmentId,
    String taskTitle,
    Integer maxPoints,
    List<TaskFileLink> taskFiles,
    List<SubmissionForReview> submissions) {
}
