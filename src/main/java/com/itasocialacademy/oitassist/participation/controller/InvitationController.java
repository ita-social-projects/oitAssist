package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessInvitationResponse;
import com.itasocialacademy.oitassist.participation.service.interfaces.InvitationService;
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
@RequestMapping("/api/v1/competitions/invitations")
@Tag(name = "Invitation Manager v1", description = "Operations related to competition invitations")
public class InvitationController {
    private final InvitationService invitationService;

    @Operation(
        summary = "Invite to the Competition",
        description = "Creates invitation requests. "
            + "The newly created invitations will initially have the PENDING status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = """
            The request was processed. Note that a 201 response does not guarantee \s
            every student was successfully invited - check the `succeeded` and `failed` \s
            fields in the response body for the per-student outcome (possible reasons: \s
            student not found, wrong role, already has a pending invitation, already a participant).
            """,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CreateInvitationResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            Could not send the invitations. Possible reasons:\s
            - The request contains duplicate student IDs.\s
            - The competition is currently not in the state to process invitations.\s
            - The specified stage ID does not belong to the competition ID.""",
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
    @PostMapping
    public ResponseEntity<CreateInvitationResponse> invite(
        @Valid @RequestBody CreateInvitationRequest createInvitationRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body((CreateInvitationResponse) invitationService.sendEnrollmentRequest(createInvitationRequest));
    }

    @Operation(
        summary = "Accept competition invitation",
        description = "Accepts the invitation and creates a Participation record.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Invitation accepted successfully. "
            + "Participation Record created successfully.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProcessInvitationResponse.class))),
        @ApiResponse(responseCode = "400", description = "The invitation request's status is not PENDING",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires USER role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The invitation was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/accept/{id}")
    public ResponseEntity<ProcessInvitationResponse> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body((ProcessInvitationResponse) invitationService.acceptRequest(id));
    }

    @Operation(
        summary = "Reject competition invitation",
        description = "Rejects the invitation with provided rejection reason.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Invitation rejected successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProcessInvitationResponse.class))),
        @ApiResponse(responseCode = "400", description = "The invitation request's status is not PENDING",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires USER role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The invitation was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/reject/{id}")
    public ResponseEntity<ProcessInvitationResponse> rejectRequest(
        @PathVariable Long id,
        @RequestBody RejectEnrollmentRequest rejectEnrollmentRequest) {
        return ResponseEntity.status(HttpStatus.OK)
            .body((ProcessInvitationResponse) invitationService.rejectRequest(id, rejectEnrollmentRequest));
    }

    @Operation(
        summary = "Cancel invitation",
        description = "Cancels the invitation for a specific user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Invitation cancelled successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProcessInvitationResponse.class))),
        @ApiResponse(responseCode = "400", description = """
            User is unable to cancel the invitation. Possible reasons:\s
            - The user is not the owner of the invitation.\s
            - The invitation request's status is not PENDING.""",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The invitation was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ORG')")
    @PatchMapping("/cancel/{id}")
    public ResponseEntity<ProcessInvitationResponse> cancelRequest(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
            .body((ProcessInvitationResponse) invitationService.cancelRequest(id));
    }
}
