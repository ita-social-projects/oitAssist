package com.itasocialacademy.oitassist.evaluation.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ResultsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Results v1", description = "Operations related to olympiad results")
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultsController {
    private final ResultsService resultsService;

    @Operation(
        summary = "Get olympiad results",
        description = "Returns paginated participant results summed per tour, stage and competition")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    // @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<PageResponse<ParticipantResult>> getResults(
        @RequestParam Long competitionId,
        @RequestParam(required = false) Set<Long> stageIds,
        @RequestParam(required = false) Set<Long> tourIds,
        @RequestParam(required = false) String search,
        @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Set<Long> stages = stageIds == null ? Set.of() : stageIds;
        Set<Long> tours = tourIds == null ? Set.of() : tourIds;
        return ResponseEntity.ok(
            PageResponse.from(resultsService.getResultsPage(competitionId, stages, tours, search, pageable)));
    }
}
