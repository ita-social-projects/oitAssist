package com.itasocialacademy.oitassist.submission.controller;

import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions V1", description = "Operations related to submissions")
public class SubmissionController {
    private final SubmissionService service;

    @Operation(
        summary = "Get submission by ID",
        description = "Returns a submission by its ID. "
            + "Only users with ADMIN or JURY role can perform this action.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubmissionResponseDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have ADMIN or JURY role",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Submission not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JURY')")
    public ResponseEntity<SubmissionResponseDTO> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSubmissionById(id));
    }

    @Operation(
        summary = "Get submission by user and task assignment",
        description = "Returns a submission created by the specified user "
            + "for the specified task assignment. "
            + "Only users with ADMIN or JURY role can perform this action.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubmissionResponseDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have ADMIN or JURY role",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Submission not found for the specified user and task assignment",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/{taskAssignmentId}/by-user-and-task-assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'JURY')")
    public ResponseEntity<SubmissionResponseDTO> getSubmissionByUserIdAndTaskAssignmentId(@PathVariable Long userId,
        @PathVariable Long taskAssignmentId) {
        return ResponseEntity.ok(service.getSubmissionBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId));
    }

    @Operation(
        summary = "Get my submission by task assignment",
        description = "Returns the current user's submission for the specified task assignment.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubmissionResponseDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have USER role",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Submission not found for the current user and task assignment",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my/{taskAssignmentId}/by-task-assignment")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubmissionResponseDTO> getMySubmissionByTaskAssignmentId(
        @PathVariable Long taskAssignmentId) {
        return ResponseEntity.ok(service.getMySubmissionByTaskAssignmentId(taskAssignmentId));
    }

    @Operation(
        summary = "Create or update submission",
        description = "Creates a new submission for the current user or updates an existing submission "
            + "for the specified task assignment. Only valid files matching the task requirements "
            + "are uploaded. Only users with USER role can perform this action.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission created or updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubmissionResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed - required files are missing or request parameters are invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have USER role",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Task assignment not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubmissionResponseDTO> postSubmission(@RequestParam(required = false) String comment,
        @RequestParam Long taskAssignmentId,
        @RequestPart("files") @NotEmpty List<MultipartFile> files) {
        return ResponseEntity.ok(service.createSubmission(comment, taskAssignmentId, files));
    }
}
