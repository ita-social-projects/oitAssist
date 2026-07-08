package com.itasocialacademy.oitassist.task.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
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
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Body Management V1", description = "Operations related to task body management")
public class TaskController {
    private final TaskService taskService;

    @Operation(
        summary = "Create a new task body",
        description = "Creates a new task body.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Task body created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN or ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TaskResponseDTO> createTask(@Valid @RequestBody CreateTaskRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    // TODO: tighten access to owner, ADMIN, or a participant with a visible
    // TaskAssignment for this task, when
    // TaskAssignment is implemented.
    @Operation(
        summary = "Get task by id",
        description = "Retrieves a specific task by its id. Requires authentication.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponseDTO> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok().body(taskService.getTaskById(taskId));
    }

    @Operation(
        summary = "Get all tasks",
        description = "Retrieves all tasks with pagination support. Requires ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<TaskResponseDTO>> getAllTasks(
        @PageableDefault(size = 15, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(taskService.getAllTasks(pageable)));
    }

    @Operation(
        summary = "Get current user's tasks",
        description = "Retrieves all tasks created by the currently authenticated user with pagination support.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User's tasks retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TaskResponseDTO>> getMyTasks(
        @PageableDefault(size = 15, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(taskService.getAllMyTasks(pageable)));
    }
}
