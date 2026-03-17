package com.itasocialacademy.oitassist.competition.controller;

import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFilter;
import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFiltersDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.ResponseCompetitionDto;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for managing competitions (olympiads). Provides
 * endpoints for creating, updating, deleting and retrieving competitions, as
 * well as retrieving competition tasks and available filter options. Base path:
 * /api/v1/competitions
 */

@Tag(name = "Competitions v1", description = "Operations related to Olympiads")
@RestController
@RequestMapping("api/v1/competitions")
public class CompetitionController
    extends
    AbstractRestControllerImpl<Long, CreateCompetitionDto, UpdateCompetitionDto, ResponseCompetitionDto, CompetitionService> {

    private final CompetitionService competitionService;

    protected CompetitionController(CompetitionService service, CompetitionService competitionService) {
        super(service);
        this.competitionService = competitionService;
    }

    /**
     * Creates a new competition. Only users with ADMIN or ORG roles are allowed to
     * perform this operation.
     *
     * @param createDto DTO containing competition creation data
     * @return created competition
     */

    @Operation(
        summary = "Create competition",
        description = "Creates a new competition. Only users with ADMIN or ORG roles are allowed.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competition created successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<ResponseCompetitionDto> save(
        @Valid @RequestBody CreateCompetitionDto createDto) {
        return super.save(createDto);
    }

    /**
     * Updates an existing competition. Only users with ADMIN or ORG roles are
     * allowed to perform this operation.
     *
     * @param updateDTO DTO containing updated competition data
     * @return updated competition
     */

    @Operation(
        summary = "Update competition",
        description = "Updates an existing competition. Only users with ADMIN or ORG roles are allowed.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competition updated successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Competition not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<ResponseCompetitionDto> update(
        @Valid @RequestBody UpdateCompetitionDto updateDTO) {
        return super.update(updateDTO);
    }

    /**
     * Deletes a competition by its ID. Only users with ADMIN or ORG roles are
     * allowed to perform this operation.
     *
     * @param id competition identifier
     * @return empty response with status 204
     */

    @Operation(
        summary = "Delete competition",
        description = "Deletes a competition by ID. Only users with ADMIN or ORG roles are allowed.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Competition deleted"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Competition not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<Void> delete(
        @PathVariable Long id) {
        return super.delete(id);
    }

    /**
     * Retrieves competition details by ID.
     *
     * @param id competition identifier
     * @return competition data
     */

    @Operation(
        summary = "Get competition by ID",
        description = "Returns competition details by ID.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competition found"),
        @ApiResponse(
            responseCode = "404",
            description = "Competition not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ResponseCompetitionDto> getById(
        @PathVariable Long id) {
        return super.getById(id);
    }

    /**
     * Retrieves a paginated list of competitions with optional filtering.
     *
     * @param filter   filtering parameters for competitions
     * @param pageable pagination information
     * @return paginated list of competitions
     */

    @Operation(
        summary = "Get all competitions",
        description = "Returns paginated list of competitions with filtering support.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competitions found"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Parameters({
        @Parameter(name = "page", description = "Zero-based page index", example = "0"),
        @Parameter(name = "size", description = "Page size", example = "5"),
        @Parameter(name = "sort", description = "Sorting criteria", example = "year,desc"),
    })
    @GetMapping()
    public ResponseEntity<PageResponse<ResponseCompetitionDto>> getAllCompetitions(
        @ParameterObject CompetitionFilter filter,
        Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(competitionService.getAllCompetitions(filter, pageable)));
    }

    /**
     * Returns available filter values for competitions. This endpoint is used by
     * the frontend to build filter UI.
     *
     * @return available competition filters
     */

    @Operation(
        summary = "Get competition filters",
        description = "Returns available filter values for competitions.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Competitions found"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/filters")
    public ResponseEntity<CompetitionFiltersDto> getFilters() {
        return ResponseEntity.ok(competitionService.getFilters());
    }

    /**
     * Retrieves all tasks that belong to a specific competition.
     *
     * @param id competition identifier
     * @return list of competition tasks
     */

    @Operation(
        summary = "Get competition tasks",
        description = "Returns list of tasks for a specific competition.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks returned successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Competition not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<ResponseTaskDTO>> getCompetitionTasks(
        @PathVariable Long id) {
        return ResponseEntity.ok(competitionService.getAllCompetitionTasks(id));
    }
}
