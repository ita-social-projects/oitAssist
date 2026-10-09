package com.itasocialacademy.oitassist.evaluation.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.evaluation.api.dto.EvaluationResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.FileLink;
import com.itasocialacademy.oitassist.evaluation.api.dto.SubmissionForReview;
import com.itasocialacademy.oitassist.evaluation.api.dto.TaskFileLink;
import com.itasocialacademy.oitassist.evaluation.api.dto.TaskReviewResponse;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewControllerTest extends ControllerUnitTest<ReviewController> {
    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private TaskReviewResponse mockTaskReview;

    @Override
    protected ReviewController getController() {
        return reviewController;
    }

    @BeforeEach
    void setUp() {
        List<TaskFileLink> taskFiles = List.of(
            new TaskFileLink(1L, "a1b2c3d4.pdf", "/api/v1/files/1", "PROBLEM"),
            new TaskFileLink(2L, "e5f6g7h8.docx", "/api/v1/files/2", "SOLUTION"));

        List<SubmissionForReview> submissions = List.of(
            new SubmissionForReview(100L, 4.5, "ok",
                List.of(new FileLink(10L, "deadbeef.pptx", "/api/v1/files/10"))),
            new SubmissionForReview(101L, null, null,
                List.of(new FileLink(11L, "cafebabe.pptx", "/api/v1/files/11"))));

        mockTaskReview = new TaskReviewResponse(
            10L, "PowerPoint Різдвяна зірка", 25, taskFiles, submissions);
    }

    // PUT /api/v1/review/submissions/{submissionId}/evaluation — evaluate

    @Test
    void evaluate_validRequest_shouldReturn200() throws Exception {
        when(reviewService.evaluate(eq(100L), eq(7.5), eq("good work")))
            .thenReturn(new EvaluationResponse(100L, 7.5, "good work"));

        mockMvc.perform(put("/api/v1/review/submissions/{submissionId}/evaluation", 100L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(evaluateRequestJson(7.5, "good work")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissionId").value(100L))
            .andExpect(jsonPath("$.score").value(7.5))
            .andExpect(jsonPath("$.comment").value("good work"));
    }

    @Test
    void evaluate_fractionalScore_shouldReturn200() throws Exception {
        when(reviewService.evaluate(eq(100L), eq(4.25), isNull()))
            .thenReturn(new EvaluationResponse(100L, 4.25, null));

        mockMvc.perform(put("/api/v1/review/submissions/{submissionId}/evaluation", 100L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(evaluateRequestJson(4.25, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(4.25));
    }

    @Test
    void evaluate_scoreOutOfRange_shouldReturn400() throws Exception {
        when(reviewService.evaluate(eq(100L), eq(99.0), isNull()))
            .thenThrow(new ValidationException(
                "Score must be between 0 and 10", ErrorCode.COMMON_VALIDATION_FAILED));

        mockMvc.perform(put("/api/v1/review/submissions/{submissionId}/evaluation", 100L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(evaluateRequestJson(99.0, null)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void evaluate_tourNotInEvaluationPhase_shouldReturn400() throws Exception {
        when(reviewService.evaluate(eq(100L), eq(5.0), isNull()))
            .thenThrow(new ValidationException(
                "Score can be set or changed only while the tour is in evaluation phase",
                ErrorCode.COMMON_VALIDATION_FAILED));

        mockMvc.perform(put("/api/v1/review/submissions/{submissionId}/evaluation", 100L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(evaluateRequestJson(5.0, null)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void evaluate_tourNotFound_shouldReturn404() throws Exception {
        when(reviewService.evaluate(eq(100L), eq(5.0), isNull()))
            .thenThrow(new TourNotFoundException(1L));

        mockMvc.perform(put("/api/v1/review/submissions/{submissionId}/evaluation", 100L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(evaluateRequestJson(5.0, null)))
            .andExpect(status().isNotFound());
    }

    @Test
    void evaluate_notAuthenticated_shouldReturn403() throws Exception {
        when(reviewService.evaluate(eq(100L), eq(5.0), isNull()))
            .thenThrow(new AuthorizationException("Not authenticated", ErrorCode.ACCESS_DENIED));

        mockMvc.perform(put("/api/v1/review/submissions/{submissionId}/evaluation", 100L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(evaluateRequestJson(5.0, null)))
            .andExpect(status().isForbidden());
    }

    // GET /api/v1/review/tasks/{taskAssignmentId} — getTaskForReview

    @Test
    void getTaskForReview_existingAssignment_shouldReturn200() throws Exception {
        when(reviewService.getTaskForReview(10L)).thenReturn(mockTaskReview);

        mockMvc.perform(get("/api/v1/review/tasks/{taskAssignmentId}", 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskAssignmentId").value(10L))
            .andExpect(jsonPath("$.taskTitle").value("PowerPoint Різдвяна зірка"))
            .andExpect(jsonPath("$.maxPoints").value(25))
            .andExpect(jsonPath("$.taskFiles").isArray())
            .andExpect(jsonPath("$.taskFiles[0].role").value("PROBLEM"))
            .andExpect(jsonPath("$.submissions").isArray())
            .andExpect(jsonPath("$.submissions.length()").value(2));
    }

    @Test
    void getTaskForReview_shouldNotExposeParticipantFilenames() throws Exception {
        when(reviewService.getTaskForReview(10L)).thenReturn(mockTaskReview);

        mockMvc.perform(get("/api/v1/review/tasks/{taskAssignmentId}", 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissions[0].files[0].storedFilename").value("deadbeef.pptx"))
            .andExpect(jsonPath("$.submissions[0].files[0].originalFilename").doesNotExist());
    }

    @Test
    void getTaskForReview_notEvaluatedSubmission_shouldReturnNullScore() throws Exception {
        when(reviewService.getTaskForReview(10L)).thenReturn(mockTaskReview);

        mockMvc.perform(get("/api/v1/review/tasks/{taskAssignmentId}", 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissions[0].score").value(4.5))
            .andExpect(jsonPath("$.submissions[1].score").doesNotExist());
    }

    @Test
    void getTaskForReview_assignmentNotFound_shouldReturn404() throws Exception {
        when(reviewService.getTaskForReview(99L))
            .thenThrow(new ValidationException(
                "Task assignment not found: 99", ErrorCode.TASK_ASSIGNMENT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/review/tasks/{taskAssignmentId}", 99L))
            .andExpect(status().isNotFound());
    }

    @Test
    void getTaskForReview_notAuthenticated_shouldReturn403() throws Exception {
        when(reviewService.getTaskForReview(10L))
            .thenThrow(new AuthorizationException("Not authenticated", ErrorCode.ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/review/tasks/{taskAssignmentId}", 10L))
            .andExpect(status().isForbidden());
    }

    private String evaluateRequestJson(Double score, String comment) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("score", score);
        body.put("comment", comment);
        return objectMapper.writeValueAsString(body);
    }
}