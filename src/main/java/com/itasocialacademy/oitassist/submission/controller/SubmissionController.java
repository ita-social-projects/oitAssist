package com.itasocialacademy.oitassist.submission.controller;

import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JURY')")
    public ResponseEntity<SubmissionResponseDTO> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSubmissionById(id));
    }

    @GetMapping("/{userId}/{taskAssignmentId}/by-user-and-task-assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'JURY')")
    public ResponseEntity<SubmissionResponseDTO> getSubmissionByUserIdAndTaskAssignmentId(@PathVariable Long userId,
                                                                                          @PathVariable
                                                                                          Long taskAssignmentId) {
        return ResponseEntity.ok(service.getSubmissionBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId));
    }

    @GetMapping("/my/{taskAssignmentId}/by-task-assignment")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubmissionResponseDTO> getMySubmissionByTaskAssignmentId(
        @PathVariable Long taskAssignmentId) {
        return ResponseEntity.ok(service.getMySubmissionByTaskAssignmentId(taskAssignmentId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubmissionResponseDTO> postSubmission(@RequestParam(required = false) String comment,
                                                                @RequestParam Long taskAssignmentId,
                                                                @RequestPart("files") @NotEmpty
                                                                List<MultipartFile> files) {
        return ResponseEntity.ok(service.createSubmission(comment, taskAssignmentId, files));
    }
}
