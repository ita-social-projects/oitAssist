package com.itasocialacademy.oitassist.competitionhistory.controller;

import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.competitionhistory.service.interfaces.CompetitionHistoryService;
import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}/competitions/history")
@Tag(name = "Competition History v1", description = "Endpoints for the user's competition history")
public class CompetitionHistoryController {
    private final CompetitionHistoryService competitionHistoryService;

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History page returned",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not the owner and not an admin",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<PageResponse<CompetitionHistoryResponse>> getHistory(
        @PathVariable Long userId,
        @ParameterObject @PageableDefault(size = 20, sort = "dateFinish",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
            PageResponse.from(competitionHistoryService.getHistory(userId, pageable)));
    }
}