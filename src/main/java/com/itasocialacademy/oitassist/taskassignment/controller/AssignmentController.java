package com.itasocialacademy.oitassist.taskassignment.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.UpdateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.service.interfaces.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Task Assignments Management V1", description = "Operations related to task linkage to tours")
public class AssignmentController {
    private final AssignmentService assignmentService;

    @Operation(
        summary = "Assign a task to a tour",
        description = "Creates a new task assignment for a specific tour. Requires ADMIN or ORG role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Task assignment created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskAssignmentResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN or ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour or task not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/tours/{tourId}/task-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TaskAssignmentResponseDTO> assignTask(@PathVariable Long tourId,
        @Valid @RequestBody CreateTaskAssignmentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.assignTask(tourId, request));
    }

    @Operation(
        summary = "Get task assignment by id",
        description = "Retrieves a specific task assignment by its id. Requires authentication.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task assignment retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskAssignmentResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Task assignment not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/task-assignments/{assignmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskAssignmentResponseDTO> getById(@PathVariable Long assignmentId) {
        return ResponseEntity.ok().body(assignmentService.getTaskAssignmentById(assignmentId));
    }

    @Operation(
        summary = "Get task assignments by tour",
        description = "Retrieves all task assignments for a specific tour. Requires authentication.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task assignments retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/tours/{tourId}/task-assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TaskAssignmentResponseDTO>> getByTour(@PathVariable Long tourId,
        @PageableDefault(size = 15, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(assignmentService.getAssignmentsByTourId(pageable, tourId)));
    }

    @Operation(
        summary = "Update a task assignment",
        description = "Updates an existing task assignment. Only users with ADMIN or ORG role can perform this action.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task assignment updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskAssignmentResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN or ORG role)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Task assignment not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/task-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TaskAssignmentResponseDTO> update(@PathVariable Long assignmentId,
        @Valid @RequestBody UpdateTaskAssignmentRequestDTO request) {
        return ResponseEntity.ok().body(assignmentService.updateTaskAssignment(assignmentId, request));
    }

    @Operation(
        summary = "Delete a task assignment",
        description = "Deletes a task assignment by its id. Only users with ADMIN or ORG role can perform this action.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task assignment deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN or ORG role)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Task assignment not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/task-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<Void> delete(@PathVariable Long assignmentId) {
        assignmentService.deleteTaskAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
