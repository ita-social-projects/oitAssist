package com.itasocialacademy.oitassist.chat.controller;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.service.interfaces.ParticipantForumService;
import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/task-assignments/{taskAssignmentId}/questions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(
    name = "Participant Forum V1",
    description = "Participant operations for TaskAssignment-scoped question forums")
public class ParticipantForumController {
    private final ParticipantForumService participantForumService;

    @Operation(
        summary = "Get task assignment forum questions",
        description = """
            Returns a paginated list of questions for the specified task assignment.

            For an authenticated participant, the response contains public questions
            and private questions created by that participant. Participant access
            requires the task assignment to be visible and requires a matching
            Participation for the assignment's competition and stage.

            A global administrator may access an existing task assignment forum
            without a participant record.

            Questions are ordered by creation time in descending order, with the
            question identifier used as a deterministic tie-breaker.
            """)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task assignment forum page retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Task assignment identifier or pagination parameters are invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "The task assignment is hidden or matching participation is missing",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "The task assignment, related tour, or related stage was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<QuestionThreadSummaryResponseDTO>> getParticipantForum(
        @Parameter(
            description = "Positive identifier of the task assignment whose forum is requested",
            example = "42",
            required = true) @PathVariable Long taskAssignmentId,
        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(defaultValue = "0") int page,
        @Parameter(
            description = "Number of questions per page. Must be between 1 and 100",
            example = "20") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            PageResponse.from(participantForumService.getForumQuestions(taskAssignmentId, page, size)));
    }

    @Operation(
        summary = "Create a question in a task assignment forum",
        description = """
            Creates a private question in the forum of the specified task assignment.

            For a participant, the task assignment must be visible and a matching
            Participation must exist for the assignment's competition and stage.
            A global administrator may access an existing task assignment forum
            without a participant record.

            The related tour must have the IN_PROGRESS execution status.

            The authenticated user becomes the question author. The backend assigns
            status NEW, state OPEN, visibility PRIVATE, no reviewer, and version zero.

            The request may provide only the question title and content. The task
            assignment identifier and all workflow fields are controlled by the
            backend.
            """)
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "201",
                description = "Question created successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        implementation = QuestionThreadResponseDTO.class))),
            @ApiResponse(
                responseCode = "400",
                description = "Task assignment identifier or request body is invalid",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        implementation = ErrorResponse.class))),
            @ApiResponse(
                responseCode = "401",
                description = "Authentication is required",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        implementation = ErrorResponse.class))),
            @ApiResponse(
                responseCode = "403",
                description = "The task assignment is hidden or matching participation is missing",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        implementation = ErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "The task assignment, related tour, or related stage was not found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        implementation = ErrorResponse.class))),
            @ApiResponse(
                responseCode = "409",
                description = "Question creation is not allowed because the related tour is not in progress",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        implementation = ErrorResponse.class)))
        })
    @PostMapping
    public ResponseEntity<QuestionThreadResponseDTO> createQuestion(
        @Parameter(
            description = "Positive identifier of the task assignment in which the question is created",
            example = "42",
            required = true) @PathVariable Long taskAssignmentId,
        @Valid @RequestBody CreateQuestionRequestDTO request) {
        QuestionThreadResponseDTO response = participantForumService.createQuestion(taskAssignmentId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}
