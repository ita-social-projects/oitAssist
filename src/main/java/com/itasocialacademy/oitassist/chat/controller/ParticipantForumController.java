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
    description = "Participant operations for temporary TaskBody-scoped question forums")
public class ParticipantForumController {
    private final ParticipantForumService participantForumService;

    @Operation(
        summary = "Get participant forum questions",
        description = """
            Returns a paginated list of questions for the specified task.

            The response contains public questions and private questions created by
            the authenticated participant. Questions are ordered by creation time
            in descending order, with the question ID used as a deterministic
            tie-breaker.

            This is a temporary TaskBody-scoped API that will be migrated to
            TaskAssignment-scoped access.
            """)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Forum questions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Task identifier or pagination parameters are invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Task was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<QuestionThreadSummaryResponseDTO>> getParticipantForum(
        @Parameter(
            description = "Positive identifier of the task whose forum is requested",
            example = "42",
            required = true) @PathVariable Long taskAssignmentId,

        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(defaultValue = "0") int page,

        @Parameter(
            description = "Number of questions per page. Must be between 1 and 100",
            example = "20") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            PageResponse.from(
                participantForumService.getForumQuestions(
                    taskAssignmentId,
                    page,
                    size)));
    }

    @Operation(
        summary = "Create a participant question",
        description = """
            Creates a private question in the forum of the specified task.

            The authenticated user becomes the question author. The backend assigns
            the initial status NEW, state OPEN, visibility PRIVATE, an empty reviewer,
            and version zero.

            The request may provide only the question title and content. This is a
            temporary TaskBody-scoped API that will be migrated to
            TaskAssignment-scoped access.
            """)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Question created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = QuestionThreadResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Task identifier or request body is invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Task was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<QuestionThreadResponseDTO> createQuestion(
        @Parameter(
            description = "Positive identifier of the task in which the question is created",
            example = "42",
            required = true) @PathVariable Long taskAssignmentId,
        @Valid @RequestBody CreateQuestionRequestDTO request) {
        QuestionThreadResponseDTO response =
            participantForumService.createQuestion(taskAssignmentId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}
