package com.itasocialacademy.oitassist.competition.controller;

import com.itasocialacademy.oitassist.competition.dto.request.ChangeTourStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.ReorderToursRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
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
@Tag(name = "Tour Management V1", description = "Operations related to competition tours")
public class TourController {
    private final TourService tourService;

    @Operation(summary = "Create a new tour")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tour created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed (e.g., date overlap)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/stages/{stageId}/tours")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TourResponse> createTour(
        @PathVariable Long stageId,
        @Valid @RequestBody CreateTourRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tourService.create(stageId, request));
    }

    @Operation(summary = "Get all tours for a stage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of tours retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stages/{stageId}/tours")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TourResponse>> getAllTours(@PathVariable Long stageId) {
        return ResponseEntity.ok(tourService.getAllByStageId(stageId));
    }

    @Operation(summary = "Update an existing tour")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tour updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/stages/{stageId}/tours/{tourId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TourResponse> updateTour(
        @PathVariable Long stageId,
        @PathVariable Long tourId,
        @Valid @RequestBody UpdateTourRequest request) {
        return ResponseEntity.ok(tourService.update(stageId, tourId, request));
    }

    @Operation(summary = "Change tour execution status manually")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tour updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/stages/{stageId}/tours/{tourId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TourResponse> changeStatus(
        @PathVariable Long stageId,
        @PathVariable Long tourId,
        @Valid @RequestBody ChangeTourStatusRequest request) {
        return ResponseEntity.ok(tourService.changeStatus(stageId, tourId, request));
    }

    @Operation(
        summary = "Reorder tours within a stage",
        description = "Reassigns tour sort positions according to the given order. The request must "
            + "list exactly the IDs of all tours currently under the stage, with no duplicates or omissions. "
            + "Not allowed once any tour in the stage has left SCHEDULED status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tours reordered successfully"),
        @ApiResponse(responseCode = "400",
            description = "Validation failed (tour set mismatch, tours already started, or hierarchy locked)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Stage not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/stages/{stageId}/tours/order")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<List<TourResponse>> reorderTours(
        @PathVariable Long stageId,
        @Valid @RequestBody ReorderToursRequest request) {
        return ResponseEntity.ok(tourService.reorder(stageId, request));
    }

    @Operation(summary = "Delete a tour")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tour deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete (competition is locked)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/stages/{stageId}/tours/{tourId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<Void> deleteTour(@PathVariable Long stageId, @PathVariable Long tourId) {
        tourService.delete(stageId, tourId);
        return ResponseEntity.noContent().build();
    }

    // flat endpoints

    @Operation(summary = "Get a specific tour by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tour retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tour not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/tours/{tourId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TourResponse> getTourById(@PathVariable Long tourId) {
        return ResponseEntity.ok(tourService.getById(tourId));
    }
}
