package com.itasocialacademy.oitassist.evaluation.controller;

import com.itasocialacademy.oitassist.evaluation.api.dto.EvaluationResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.TaskReviewResponse;
import com.itasocialacademy.oitassist.evaluation.dao.dto.request.EvaluateSubmissionRequest;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Review v1", description = "Jury evaluation of submissions")
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(
        summary = "Set or update a score for a submission",
        description = "Creates an evaluation for the submission or overwrites the existing one. "
            + "Score changes are logged with the jury member id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Score saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid score"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/submissions/{submissionId}/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN', 'JURY')")
    public ResponseEntity<EvaluationResponse> evaluate(
        @PathVariable Long submissionId,
        @RequestBody EvaluateSubmissionRequest request) {
        return ResponseEntity.ok(reviewService.evaluate(submissionId, request.score(), request.comment()));
    }

    @Operation(
        summary = "Get a task assignment for jury review",
        description = "Returns the reference solution files and anonymized submissions "
            + "with their current scores")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Task assignment not found")
    })
    @GetMapping("/tasks/{taskAssignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JURY')")
    public ResponseEntity<TaskReviewResponse> getTaskForReview(@PathVariable Long taskAssignmentId) {
        return ResponseEntity.ok(reviewService.getTaskForReview(taskAssignmentId));
    }
}
