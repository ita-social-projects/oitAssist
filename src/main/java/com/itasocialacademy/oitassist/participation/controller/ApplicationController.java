package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.request.AcceptApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.*;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Application Manager v1", description = "Operations related to competition applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    @Operation(
        summary = "Apply to the Competition",
        description = "Creates an application request. "
            + "The newly created application will initially have the PENDING status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Application submitted successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CreateApplicationResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            User is unable to apply. Possible reasons:\s
            - User already has a pending request for this stage.\s
            - User is already a participant in this stage.\s
            - The competition is not currently accepting applications.\s
            - Sending application requests is limited to District and City stages.\s
            - The specified stage ID does not belong to the competition ID.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires USER role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = """
            Resource missing. Possible reasons:\s
            - The requested competition does not exist.\s
            - The requested stage does not exist.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/competitions/{competitionId}/stages/{stageId}/applications")
    public ResponseEntity<CreateApplicationResponse> apply(
        @PathVariable Long competitionId,
        @PathVariable Long stageId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body((CreateApplicationResponse) applicationService.sendApplicationRequest(competitionId, stageId));
    }

    @Operation(
        summary = "Accept user's application",
        description = "Accepts the application and creates a Participation record. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Application accepted successfully. "
            + "Participation Record created successfully.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProcessApplicationResponse.class))),
        @ApiResponse(responseCode = "400", description = "The application request's status is not PENDING",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The application was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ORG')")
    @PostMapping("/enrollment/applications/{id}/accept")
    public ResponseEntity<ProcessApplicationResponse> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body((ProcessApplicationResponse) applicationService.acceptRequest(id));
    }

    @Operation(
        summary = "Accept users' applications",
        description = "Accepts the application list and creates Participation records.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = """
            The request was processed. Note that a 201 response does not guarantee \s
            every application was successfully accepted - check the `succeeded` and `failed` \s
            fields in the response body for the per-student outcome (possible reasons: \s
            application not found, the request's status is not PENDING, applicant is already a participant).
            """,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AcceptedApplicationListResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            Could not accept applications. Possible reasons:\s
            - The request contains duplicate application IDs.\s
            - Applications don't belong to the same competition stage.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No requested application was found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ORG')")
    @PostMapping("/enrollment/applications/accept-batch")
    public ResponseEntity<AcceptedApplicationListResponse> acceptRequests(
        @Valid @RequestBody AcceptApplicationListRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(applicationService.acceptApplicationList(request));
    }

    @Operation(
        summary = "Reject user's application",
        description = "Rejects the application with provided rejection reason.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application rejected successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProcessApplicationResponse.class))),
        @ApiResponse(responseCode = "400", description = "The application request's status is not PENDING",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The application was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ORG')")
    @PatchMapping("/enrollment/applications/{id}/reject")
    public ResponseEntity<ProcessApplicationResponse> rejectRequest(
        @PathVariable Long id,
        @RequestBody RejectEnrollmentRequest rejectEnrollmentRequest) {
        return ResponseEntity.status(HttpStatus.OK)
            .body((ProcessApplicationResponse) applicationService.rejectRequest(id, rejectEnrollmentRequest));
    }

    @Operation(
        summary = "Reject users' applications",
        description = "Rejects the application list with provided rejection reason.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = """
            The request was processed. Note that a 201 response does not guarantee \s
            every application was successfully rejected - check the `succeeded` and `failed` \s
            fields in the response body for the per-student outcome (possible reasons: \s
            application not found, the request's status is not PENDING).
            """,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = RejectedApplicationListResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            Could not reject applications. Possible reasons:\s
            - The request contains duplicate application IDs.\s
            - Applications don't belong to the same competition stage.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No requested application was found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ORG')")
    @PostMapping("/enrollment/applications/reject-batch")
    public ResponseEntity<RejectedApplicationListResponse> rejectRequests(
        @Valid @RequestBody RejectApplicationListRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(applicationService.rejectApplicationList(request));
    }

    @Operation(
        summary = "Cancel user's application",
        description = "Cancels the application from a specific user. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application cancelled successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProcessApplicationResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            User is unable to cancel the request. Possible reasons:\s
            - The user is not the owner of the application.\s
            - The application request's status is not PENDING.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires USER role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The application was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/enrollment/applications/{id}/cancel")
    public ResponseEntity<ProcessApplicationResponse> cancelRequest(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
            .body((ProcessApplicationResponse) applicationService.cancelRequest(id));
    }

    @Operation(
        summary = "Get users' application requests",
        description = "Retrieves a paginated list of application requests "
            + "for a specific competition stage.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            The competition and stage info error. The reason: \s
            specified stage ID does not belong to the competition ID.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = """
            Resource missing. Possible reasons:\s
            - The requested competition does not exist.\s
            - The requested stage does not exist.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ORG')")
    @GetMapping("/competitions/{competitionId}/stages/{stageId}/applications")
    public ResponseEntity<PageResponse<ApplicationListItemResponse>> getRequests(
        @PathVariable Long competitionId,
        @PathVariable Long stageId,
        @RequestParam(required = false) String search,
        @ParameterObject @PageableDefault(size = 20, sort = "issuedAt") Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(PageResponse.from(applicationService.getEnrollmentRequests(
                competitionId, stageId, search, pageable)));
    }
}
