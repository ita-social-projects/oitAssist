package com.itasocialacademy.oitassist.competition.controller;

import com.itasocialacademy.oitassist.competition.dto.request.ChangeCompetitionStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionTreeResponse;
import com.itasocialacademy.oitassist.competition.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
@Tag(name = "Competition Management V1", description = "Operations related to competition management")
public class CompetitionController {
    private final CompetitionService competitionService;

    @Operation(
        summary = "Create a new competition",
        description = "Creates a new competition structure. "
            + "The newly created competition will initially have the DRAFT status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Competition created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CompetitionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN or ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<CompetitionResponse> createCompetition(@Valid @RequestBody CreateCompetitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competitionService.create(request));
    }

    @Operation(
        summary = "Get all visible competitions",
        description = "Retrieves a paginated list of competitions. "
            + "Visibility depends on user role (e.g., USER sees only PUBLISHED/ARCHIVED, ORG sees their DRAFTs).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competitions retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CompetitionResponse>> getAllVisible(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(competitionService.getAllVisible(pageable)));
    }

    @Operation(
        summary = "Get archived competitions",
        description = "Retrieves a paginated list of competitions that have been archived. "
            + "Accessed via a separate endpoint as per archive policy.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Archived competitions retrieved successfully")
    })
    @GetMapping("/archived")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CompetitionResponse>> getArchived(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(competitionService.getArchived(pageable)));
    }

    @Operation(
        summary = "Get competition details by ID",
        description = "Retrieves details of a specific competition. "
            + "Access to DRAFT competitions is restricted to ADMINs or the ORG.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competition retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CompetitionResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (e.g., attempt to view another's DRAFT)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Competition not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{competitionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompetitionResponse> getCompetitionById(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionService.getVisibleById(competitionId));
    }

    @Operation(
        summary = "Get full competition tree (Competition -> Stages -> Tours)",
        description = "Retrieves the competition together with its full hierarchy of stages and tours "
            + "in a single nested response. Access to a DRAFT competition's tree follows the same "
            + "visibility rules as viewing the competition itself.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competition tree retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CompetitionTreeResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (e.g., attempt to view another's DRAFT)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Competition not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{competitionId}/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompetitionTreeResponse> getCompetitionTree(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionService.getCompetitionTree(competitionId));
    }

    @Operation(
        summary = "Change competition status",
        description = "Transitions the competition to a new status. "
            + "Publishing requires at least one Stage and one Tour. Archived competitions are read-only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CompetitionResponse.class))),
        @ApiResponse(responseCode = "400",
            description = "Hierarchy validation failed "
                + "(e.g., trying to publish an empty competition or invalid state transition)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN or ORG role)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Competition not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{competitionId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<CompetitionResponse> changeStatus(
        @PathVariable Long competitionId,
        @Valid @RequestBody ChangeCompetitionStatusRequest request) {
        return ResponseEntity.ok(competitionService.changeStatus(competitionId, request.status()));
    }
}
