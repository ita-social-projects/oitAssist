package com.itasocialacademy.oitassist.competition.controller;

import com.itasocialacademy.oitassist.competition.dto.request.ChangeStageStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Stage Management V1", description = "Operations related to competition stages")
public class StageController {
    private final StageService stageService;

    @Operation(
        summary = "Create a new stage",
        description = "Adds a new stage to a competition's hierarchy. "
            + "Stage dates must fall within the parent competition's date range, and the competition "
            + "must not be ARCHIVED (or PUBLISHED/FINISHED with active participations).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Stage created successfully"),
        @ApiResponse(responseCode = "400",
            description = "Validation failed (e.g., dates outside competition range, duplicate title/sort "
                + "position, or competition hierarchy is locked)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN or ORG role)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Competition not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/competitions/{competitionId}/stages")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<StageResponse> createStage(
        @PathVariable Long competitionId,
        @Valid @RequestBody CreateStageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(stageService.create(competitionId, request));
    }

    @Operation(summary = "Get all stages for a competition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of stages retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Stage not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/competitions/{competitionId}/stages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StageResponse>> getAllStages(@PathVariable Long competitionId) {
        return ResponseEntity.ok(stageService.getAllByCompetitionId(competitionId));
    }

    @Operation(summary = "Update an existing stage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stage updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed (e.g., date overlap or locked competition)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Stage not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/competitions/{competitionId}/stages/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<StageResponse> updateStage(
        @PathVariable Long competitionId,
        @PathVariable Long stageId,
        @Valid @RequestBody UpdateStageRequest request) {
        return ResponseEntity.ok(stageService.update(competitionId, stageId, request));
    }

    @Operation(
        summary = "Change stage status manually",
        description = "Transitions the stage to a new status. Cannot start if the previous stage is not FINISHED.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stage status updated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Stage not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    @PatchMapping("/competitions/{competitionId}/stages/{stageId}/status")
    public ResponseEntity<StageResponse> changeStatus(
        @PathVariable Long competitionId,
        @PathVariable Long stageId,
        @Valid @RequestBody ChangeStageStatusRequest request) {
        return ResponseEntity.ok(stageService.changeStatus(competitionId, stageId, request));
    }

    @Operation(summary = "Delete a stage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Stage deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete (competition is locked)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Stage not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/competitions/{competitionId}/stages/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<Void> deleteStage(@PathVariable Long competitionId, @PathVariable Long stageId) {
        stageService.delete(competitionId, stageId);
        return ResponseEntity.noContent().build();
    }

    // flat endpoints

    @Operation(summary = "Get a specific stage by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stage retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete (competition is locked)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Stage not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stages/{stageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StageResponse> getStageById(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.getById(stageId));
    }
}
