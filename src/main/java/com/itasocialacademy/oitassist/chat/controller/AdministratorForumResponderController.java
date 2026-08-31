package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderGrantResult;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/task-assignments/{taskAssignmentId}/forum-responders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(
    name = "Administrator Forum Responders V1",
    description = """
        Administrator operations for managing TaskAssignment-specific
        organizing committee forum responders
        """)
public class AdministratorForumResponderController {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final TaskAssignmentForumResponderService responderService;

    @Operation(
        summary = "List TaskAssignment forum responders",
        description = """
            Returns responders assigned to one exact TaskAssignment.

            Results are ordered by assignedAt DESC and id DESC.
            TaskAssignments sharing one TaskBody remain independent.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Responder page retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Identifier or pagination is invalid",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Global administrator role is required",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "TaskAssignment or target user was not found",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<TaskAssignmentForumResponderResponseDTO>> getResponders(
        @Parameter(
            description = "Positive TaskAssignment identifier",
            example = "120") @PathVariable Long taskAssignmentId,

        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(defaultValue = "0") int page,

        @Parameter(
            description = "Page size within configured limit",
            example = "20") @RequestParam(defaultValue = "20") int size) {
        validateIdentifier(
            taskAssignmentId,
            "Task assignment id");

        validatePageAndSize(
            page,
            size);

        return ResponseEntity.ok(
            PageResponse.from(
                responderService.getResponders(
                    taskAssignmentId,
                    page,
                    size)));
    }

    @Operation(
        summary = "Grant forum responder eligibility",
        description = """
            Assigns one active ORG user as a forum responder for one exact
            TaskAssignment.

            The operation is idempotent and does not claim or modify any
            question.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Responder assignment created",
            content = @Content(
                schema = @Schema(
                    implementation = TaskAssignmentForumResponderResponseDTO.class))),
        @ApiResponse(
            responseCode = "200",
            description = "Responder assignment already existed",
            content = @Content(
                schema = @Schema(
                    implementation = TaskAssignmentForumResponderResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Identifier or responder candidate is invalid",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Global administrator role is required",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "TaskAssignment or user was not found",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @PutMapping("/{userId}")
    public ResponseEntity<TaskAssignmentForumResponderResponseDTO> grantResponder(
        @PathVariable Long taskAssignmentId,
        @PathVariable Long userId) {
        validateIdentifier(
            taskAssignmentId,
            "Task assignment id");

        validateIdentifier(
            userId,
            "Responder user id");

        TaskAssignmentForumResponderGrantResult result =
            responderService.grantResponder(
                taskAssignmentId,
                userId);

        HttpStatus status =
            result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity
            .status(status)
            .body(result.responder());
    }

    @Operation(
        summary = "Revoke forum responder eligibility",
        description = """
            Removes TaskAssignment-specific responder eligibility.

            Repeated revocation is idempotent. Revocation is rejected when
            the responder owns an active open review.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Responder eligibility removed or absent"),
        @ApiResponse(
            responseCode = "400",
            description = "Identifier is invalid",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Global administrator role is required",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "TaskAssignment was not found",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Responder owns an active open review",
            content = @Content(
                schema = @Schema(
                    implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> revokeResponder(
        @PathVariable Long taskAssignmentId,
        @PathVariable Long userId) {
        validateIdentifier(
            taskAssignmentId,
            "Task assignment id");

        validateIdentifier(
            userId,
            "Responder user id");

        responderService.revokeResponder(
            taskAssignmentId,
            userId);

        return ResponseEntity.noContent().build();
    }

    private void validateIdentifier(
        Long identifier,
        String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new ValidationException(
                "%s must be a positive number"
                    .formatted(fieldName),
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