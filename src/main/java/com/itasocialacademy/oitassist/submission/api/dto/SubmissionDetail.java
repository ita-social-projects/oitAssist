package com.itasocialacademy.oitassist.submission.api.dto;

import java.time.Instant;
import lombok.Builder;

/**
 * DTO representing the submission for cross-module communication.
 */
@Builder
public record SubmissionDetail(
    Long id,
    String comment,
    Instant submittedAt,
    Long submittedBy,
    Long taskAssignmentId) {
}
