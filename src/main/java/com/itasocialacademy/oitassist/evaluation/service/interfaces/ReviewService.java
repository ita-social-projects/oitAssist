package com.itasocialacademy.oitassist.evaluation.service.interfaces;

import com.itasocialacademy.oitassist.evaluation.api.dto.EvaluationResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.TaskReviewResponse;

public interface ReviewService {
    EvaluationResponse evaluate(Long submissionId, Double score, String comment);

    TaskReviewResponse getTaskForReview(Long taskAssignmentId);
}