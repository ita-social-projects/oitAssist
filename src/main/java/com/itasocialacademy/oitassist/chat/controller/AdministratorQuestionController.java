package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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