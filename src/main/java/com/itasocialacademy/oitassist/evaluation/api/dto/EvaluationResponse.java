package com.itasocialacademy.oitassist.evaluation.api.dto;

/**
 * Result of a jury evaluation: the submission and its current score.
 */
public record EvaluationResponse(
    Long submissionId,
    Double score,
    String comment) {
}
