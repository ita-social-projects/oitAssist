package com.itasocialacademy.oitassist.chat.controller;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateCommentRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.service.interfaces.ParticipantQuestionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(
        name = "Participant Questions V1",
        description = "Participant operations for question threads and messages")
public class ParticipantQuestionController {
    private final ParticipantQuestionService participantQuestionService;

    @Operation(
            summary = "Get question details",
            description = """
            Returns the complete details of an accessible question thread.

            Access depends on the related TaskAssignment, question visibility,
            authorship, administrator access and assigned-reviewer access.
            Private questions are returned only to authorized users. Closed
            questions remain readable.
            """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Question details retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            QuestionThreadResponseDTO.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Question identifier is invalid",
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
                    description = "Access to the related task assignment is restricted",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Question was not found or was masked",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class)))
    })
    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionThreadResponseDTO>
    getQuestionDetails(
            @Parameter(
                    description = "Positive identifier of the question thread",
                    example = "42",
                    required = true)
            @PathVariable Long questionId) {
        return ResponseEntity.ok(
                participantQuestionService
                        .getQuestionDetails(questionId));
    }

    @Operation(
            summary = "Get question message history",
            description = """
            Returns a paginated history of messages belonging to an accessible
            question thread.

            Access is validated before message content is retrieved. Messages are
            ordered by createdAt ASC and id ASC. Supported message types are
            COMMENT and OFFICIAL_ANSWER.
            """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Message history retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PageResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Question identifier or pagination is invalid",
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
                    description = "Access to the related task assignment is restricted",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Question was not found or was masked",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class)))
    })
    @GetMapping("/{questionId}/messages")
    public ResponseEntity<PageResponse<QuestionMessageResponseDTO>>
    getQuestionMessages(
            @Parameter(
                    description = "Positive identifier of the question thread",
                    example = "42",
                    required = true)
            @PathVariable Long questionId,

            @Parameter(
                    description = "Zero-based page number",
                    example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    description = "Page size between 1 and 100",
                    example = "50")
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
                PageResponse.from(
                        participantQuestionService
                                .getQuestionMessages(
                                        questionId,
                                        page,
                                        size)));
    }

    @Operation(
            summary = "Add a participant comment",
            description = """
            Creates a comment for an accessible open question thread.

            The request accepts only comment content. The authenticated user
            becomes the comment author, and the backend always assigns message
            type COMMENT. Official answers cannot be created through this
            endpoint. Closed questions do not accept new comments.
            """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Comment created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            QuestionMessageResponseDTO.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Question identifier or comment content is invalid",
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
                    description = "Participation in the question is restricted",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Question was not found or was masked",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "The question is closed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class)))
    })
    @PostMapping("/{questionId}/comments")
    public ResponseEntity<QuestionMessageResponseDTO>
    addComment(
            @Parameter(
                    description = "Positive identifier of the question thread",
                    example = "42",
                    required = true)
            @PathVariable Long questionId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Comment content",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            CreateCommentRequestDTO.class)))
            @Valid
            @RequestBody
            CreateCommentRequestDTO request) {

        QuestionMessageResponseDTO response =
                participantQuestionService.addComment(
                        questionId,
                        request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}