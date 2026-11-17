package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.InvitationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.service.interfaces.StudentEnrollmentHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}")
@Tag(name = "Student Enrollment History v1",
    description = "Operations related to the enrollment history of a student")
public class StudentEnrollmentHistoryController {
    private final StudentEnrollmentHistoryService historyService;

    @Operation(
        summary = "Get student's invitations",
        description = "Retrieves a paginated list of the student's invitations. "
            + "Returns all statuses unless the status parameter is provided.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status value",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (not the owner and not an organizer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('USER','ORG')")
    @GetMapping("/invitations")
    public ResponseEntity<PageResponse<InvitationHistoryItemResponse>> getInvitations(
        @PathVariable Long userId,
        @RequestParam(required = false) RequestStatus status,
        @ParameterObject @PageableDefault(size = 20, sort = "issuedAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(PageResponse.from(historyService.getInvitations(userId, status, pageable)));
    }

    @Operation(
        summary = "Get student's applications",
        description = "Retrieves a paginated list of the student's applications. "
            + "Returns all statuses unless the status parameter is provided.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status value",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (not the owner and not an organizer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('USER','ORG')")
    @GetMapping("/applications")
    public ResponseEntity<PageResponse<ApplicationHistoryItemResponse>> getApplications(
        @PathVariable Long userId,
        @RequestParam(required = false) RequestStatus status,
        @ParameterObject @PageableDefault(size = 20, sort = "issuedAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(PageResponse.from(historyService.getApplications(userId, status, pageable)));
    }

    @Operation(
        summary = "Get student's participations",
        description = "Retrieves a paginated list of the student's confirmed participations.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Participations retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (not the owner and not an organizer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('USER','ORG')")
    @GetMapping("/participations")
    public ResponseEntity<PageResponse<ParticipationHistoryItemResponse>> getParticipations(
        @PathVariable Long userId,
        @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(PageResponse.from(historyService.getParticipations(userId, pageable)));
    }
}