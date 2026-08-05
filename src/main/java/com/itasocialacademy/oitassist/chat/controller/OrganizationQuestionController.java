package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.request.ClaimQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.service.interfaces.OrganizationQuestionService;
import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/org/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG')")
@Tag(
    name = "Organization Questions V1",
    description = """
        TaskAssignment-scoped question review queues for organizing
        committee responders
        """)
public class OrganizationQuestionController {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OrganizationQuestionService organizationQuestionService;

    @Operation(
        summary = "Get responder-scoped question inbox",
        description = """
            Returns open, new and unassigned questions from exact
            TaskAssignments for which the authenticated ORG user has
            responder eligibility.

            Question visibility does not affect responder eligibility.
            Results are ordered by createdAt ASC and id ASC.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Responder inbox retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Pagination parameters are invalid",
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
            description = "Global ORG role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @GetMapping("/inbox")
    public ResponseEntity<PageResponse<QuestionReviewInboxItemResponseDTO>> getResponderInbox(
        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(defaultValue = "0") int page,

        @Parameter(
            description = "Page size within the configured limit",
            example = "20") @RequestParam(defaultValue = "20") int size) {
        validatePageAndSize(
            page,
            size);

        return ResponseEntity.ok(
            PageResponse.from(
                organizationQuestionService
                    .getResponderInbox(
                        page,
                        size)));
    }

    @Operation(
        summary = "Get questions assigned to current responder",
        description = """
            Returns open questions assigned to the authenticated ORG
            responder.

            The optional status filter supports NEW, IN_REVIEW and ANSWERED.
            Results are ordered by updatedAt DESC and id DESC.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Assigned question page retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Pagination or status parameter is invalid",
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
            description = "Global ORG role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @GetMapping("/assigned-to-me")
    public ResponseEntity<PageResponse<QuestionReviewInboxItemResponseDTO>> getAssignedToCurrentResponder(
        @Parameter(
            description = "Optional question-status filter",
            example = "IN_REVIEW") @RequestParam(required = false) QuestionStatus status,

        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(defaultValue = "0") int page,

        @Parameter(
            description = "Page size within the configured limit",
            example = "20") @RequestParam(defaultValue = "20") int size) {
        validatePageAndSize(
            page,
            size);

        return ResponseEntity.ok(
            PageResponse.from(
                organizationQuestionService
                    .getAssignedToCurrentResponder(
                        status,
                        page,
                        size)));
    }

    @Operation(
        summary = "Claim a question as an ORG responder",
        description = """
            Atomically assigns an eligible open, new and unclaimed question
            to the authenticated organizing-committee responder.

            The caller must have responder eligibility for the question's
            exact TaskAssignment. The backend controls reviewer assignment,
            status transition and version increment.

            Missing and inaccessible questions use the same not-found
            response to avoid exposing protected question information.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Question claimed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = QuestionThreadResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Question id or expected version is invalid",
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
            description = "Global ORG role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = """
                Question was not found or is outside the current
                responder's TaskAssignment scope
                """,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = """
                Question is already assigned, is closed, is not NEW or its
                persisted version differs from the expected version
                """,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @PostMapping("/{questionId}/claim")
    public ResponseEntity<QuestionThreadResponseDTO> claimQuestion(
        @Parameter(
            description = "Positive question identifier",
            example = "42",
            required = true) @PathVariable Long questionId,

        @Valid @RequestBody ClaimQuestionRequestDTO request) {
        validateQuestionId(
            questionId);

        return ResponseEntity.ok(
            organizationQuestionService
                .claimQuestion(
                    questionId,
                    request.version()));
    }

    private void validateQuestionId(
        Long questionId) {
        if (questionId == null
            || questionId <= 0) {
            throw new ValidationException(
                "Question id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validatePageAndSize(
        int page,
        int size) {
        if (page < 0) {
            throw new ValidationException(
                "Page number must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException(
                "Page size must be between 1 and %d"
                    .formatted(MAX_PAGE_SIZE),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }
}