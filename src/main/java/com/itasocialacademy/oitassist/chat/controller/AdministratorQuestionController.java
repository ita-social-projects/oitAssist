package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.service.interfaces.AdministratorQuestionService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.itasocialacademy.oitassist.chat.dao.dto.request.ClaimQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStateRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStatusRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionVisibilityRequestDTO;
import org.springframework.web.bind.annotation.PatchMapping;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(
    name = "Administrator Questions V1",
    description = "Administrator question inbox and review operations")
public class AdministratorQuestionController {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdministratorQuestionService administratorQuestionService;

    @Operation(
        summary = "Get unclaimed question inbox",
        description = """
            Returns open, new and unassigned questions available for
            administrator review.

            Results include both private and public questions and are ordered
            by createdAt ASC and id ASC.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Unclaimed question page retrieved successfully",
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
            description = "Global administrator role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @GetMapping("/inbox")
    public ResponseEntity<PageResponse<AdminQuestionInboxItemResponseDTO>> getUnclaimedQuestions(
        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(
                defaultValue = "0") int page,

        @Parameter(
            description = "Page size within the configured limit",
            example = "20") @RequestParam(
                defaultValue = "20") int size) {
        validatePageAndSize(
            page,
            size);
        return ResponseEntity.ok(
            PageResponse.from(
                administratorQuestionService
                    .getUnclaimedQuestions(
                        page,
                        size)));
    }

    @Operation(
        summary = "Get questions assigned to current administrator",
        description = """
            Returns open questions assigned to the authenticated global
            administrator.

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
            description = "Global administrator role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @GetMapping("/assigned-to-me")
    public ResponseEntity<PageResponse<AdminQuestionInboxItemResponseDTO>> getAssignedQuestions(
        @Parameter(
            description = "Optional question-status filter",
            example = "IN_REVIEW") @RequestParam(
                required = false) QuestionStatus status,

        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(
                defaultValue = "0") int page,

        @Parameter(
            description = "Page size within the configured limit",
            example = "20") @RequestParam(
                defaultValue = "20") int size) {
        validatePageAndSize(
            page,
            size);
        return ResponseEntity.ok(
            PageResponse.from(
                administratorQuestionService
                    .getAssignedQuestions(
                        status,
                        page,
                        size)));
    }

    @Operation(
        summary = "Claim a question for review",
        description = """
            Atomically assigns an eligible open and unclaimed question to the
            authenticated global administrator.

            The operation is non-idempotent. The backend assigns the reviewer,
            changes the status to IN_REVIEW and increments the version.

            A concurrent or otherwise unsuccessful claim returns a classified
            conflict response and is not retried automatically.
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
            description = "Global administrator role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Question was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = """
                Question is already claimed, is not claimable or its version
                conflicts with the persisted version
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
            example = "42") @PathVariable Long questionId,
        @Valid @RequestBody ClaimQuestionRequestDTO request) {
        validateQuestionId(questionId);

        return ResponseEntity.ok(
            administratorQuestionService
                .claimQuestion(
                    questionId,
                    request.version()));
    }

    @Operation(
        summary = "Publish an official answer",
        description = """
            Publishes an official answer in an open question thread.

            Claiming the question is not required, and reviewer assignment does
            not restrict publication. Questions in NEW or IN_REVIEW status
            transition to ANSWERED. Additional official answers are allowed for
            questions already in ANSWERED status.

            The authenticated global administrator becomes the message author.
            The backend controls the author id, question id, message type,
            identifier and creation timestamp.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Official answer published successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = QuestionMessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Question identifier or answer content is invalid",
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
            description = "Global administrator role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Question was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = """
                Question is closed or a concurrent lifecycle operation
                completed before publication
                """,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @PostMapping("/{questionId}/official-answers")
    public ResponseEntity<QuestionMessageResponseDTO> publishOfficialAnswer(
        @Parameter(
            description = "Positive question identifier",
            example = "42",
            required = true) @PathVariable Long questionId,
        @Valid @RequestBody CreateOfficialAnswerRequestDTO request) {
        validateQuestionId(questionId);

        QuestionMessageResponseDTO response =
            administratorQuestionService
                .publishOfficialAnswer(
                    questionId,
                    request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @Operation(
        summary = "Change question visibility",
        description = """
            Changes question visibility independently of its review status,
            lifecycle state and reviewer assignment.

            The request must contain the expected current version. A successful
            update increments the version exactly once. A stale version produces
            QUESTION_VERSION_CONFLICT and is not retried automatically.

            Changing visibility to PUBLIC makes the question available through
            existing participant-facing access rules. Changing it to PRIVATE hides
            it from unauthorized participants.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Question visibility updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = QuestionThreadResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Question id, visibility or version is invalid",
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
            description = "Global administrator role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Question was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Supplied question version is stale",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{questionId}/visibility")
    public ResponseEntity<QuestionThreadResponseDTO> updateVisibility(
        @Parameter(
            description = "Positive question identifier",
            example = "42",
            required = true) @PathVariable Long questionId,
        @Valid @RequestBody UpdateQuestionVisibilityRequestDTO request) {
        validateQuestionId(questionId);

        return ResponseEntity.ok(
            administratorQuestionService
                .updateVisibility(
                    questionId,
                    request));
    }

    @Operation(
        summary = "Change question review status",
        description = """
            Administratively overrides the question review status.

            Supported values are NEW, IN_REVIEW and ANSWERED. The operation is
            independent of lifecycle state and visibility. It does not assign,
            replace or remove the reviewer and does not create or delete messages.

            The expected current version is required. A successful update increments
            the version exactly once. A stale request returns
            QUESTION_VERSION_CONFLICT without an automatic retry.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Question status updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = QuestionThreadResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Question id, status or version is invalid",
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
            description = "Global administrator role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Question was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Supplied question version is stale",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{questionId}/status")
    public ResponseEntity<QuestionThreadResponseDTO> updateStatus(
        @Parameter(
            description = "Positive question identifier",
            example = "42",
            required = true) @PathVariable Long questionId,
        @Valid @RequestBody UpdateQuestionStatusRequestDTO request) {
        validateQuestionId(questionId);

        return ResponseEntity.ok(
            administratorQuestionService
                .updateStatus(
                    questionId,
                    request));
    }

    @Operation(
        summary = "Change question lifecycle state",
        description = """
            Closes or reopens a question independently of visibility, review status
            and reviewer assignment.

            Closed questions remain readable through permitted question-details and
            message-history flows but reject participant comments and official
            answers. Reopening permits new messages again according to their normal
            authorization rules.

            The expected current version is required. A successful update increments
            the version exactly once. A stale request returns
            QUESTION_VERSION_CONFLICT without an automatic retry.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Question lifecycle state updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = QuestionThreadResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Question id, state or version is invalid",
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
            description = "Global administrator role is required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Question was not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Supplied question version is stale",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{questionId}/state")
    public ResponseEntity<QuestionThreadResponseDTO> updateState(
        @Parameter(
            description = "Positive question identifier",
            example = "42",
            required = true) @PathVariable Long questionId,
        @Valid @RequestBody UpdateQuestionStateRequestDTO request) {
        validateQuestionId(questionId);

        return ResponseEntity.ok(
            administratorQuestionService
                .updateState(
                    questionId,
                    request));
    }

    private void validateQuestionId(Long questionId) {
        if (questionId == null || questionId <= 0) {
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