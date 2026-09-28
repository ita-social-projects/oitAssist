package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationListItemResponse;
import com.itasocialacademy.oitassist.participation.service.interfaces.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/competitions/{competitionId}/stages/{stageId}")
@Tag(name = "Participation Manager v1", description = "Operations related to participants")
public class ParticipationController {
    private final ParticipationService participationService;

    @Operation(
        summary = "Get participation list",
        description = "Retrieves a paginated list of participants for a specific competition stage.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Participants retrieved successfully",
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
    @GetMapping("/participants")
    public ResponseEntity<PageResponse<ParticipationListItemResponse>> getRequests(
        @PathVariable Long competitionId,
        @PathVariable Long stageId,
        @RequestParam(required = false) String search,
        @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(PageResponse.from(
                participationService.getParticipationList(competitionId, stageId, search, pageable)));
    }
}
