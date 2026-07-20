package com.itasocialacademy.oitassist.taskassignment.controller;

import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.service.interfaces.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentService assignmentService;

    @PostMapping("/tours/{tourId}/task-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TaskAssignmentResponseDTO> assignTask(@PathVariable Long tourId,
        @Valid @RequestBody CreateTaskAssignmentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.assignTask(tourId, request));
    }
}
