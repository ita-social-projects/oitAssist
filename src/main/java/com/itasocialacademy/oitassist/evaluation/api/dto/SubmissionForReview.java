package com.itasocialacademy.oitassist.evaluation.api.dto;

import java.util.List;

/**
 * Submission list item for jury review. Deliberately excludes participant
 * identity (userId, original filenames) to preserve anonymity.
 */
public record SubmissionForReview(
    Long submissionId,
    Double score,
    String comment,
    List<FileLink> files) {
}
