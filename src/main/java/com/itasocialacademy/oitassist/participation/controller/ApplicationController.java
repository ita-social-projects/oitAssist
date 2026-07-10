package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessApplicationResponse;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/competitions/applications")
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
    @PostMapping
    public ResponseEntity<CreateApplicationResponse> apply(
        @Valid @RequestBody CreateApplicationRequest createApplicationRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(applicationService.userApply(createApplicationRequest));
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
    @PostMapping("/accept/{id}")
    public ResponseEntity<ProcessApplicationResponse> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(applicationService.acceptUserApplication(id));
    }

    @Operation(
        summary = "Reject user's application",
        description = "Rejects the application with provided rejection reason. ")
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
    @PatchMapping("/reject/{id}")
    public ResponseEntity<ProcessApplicationResponse> rejectRequest(
        @PathVariable Long id,
        @RequestBody RejectApplicationRequest rejectApplicationRequest) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(applicationService.rejectUserApplication(id, rejectApplicationRequest));
    }

    @Operation(
        summary = "Cancel user's application",
        description = "Cancels the application from a specific user. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application cancelled successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProcessApplicationResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            User is unable to apply. Possible reasons:\s
            - The user is not the owner of the application.\s
            - The application request's status is not PENDING.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires USER role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The application was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/cancel/{id}")
    public ResponseEntity<ProcessApplicationResponse> cancelRequest(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(applicationService.cancelUserApplication(id));
    }
}
