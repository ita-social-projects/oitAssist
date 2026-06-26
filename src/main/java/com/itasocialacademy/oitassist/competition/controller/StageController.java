package com.itasocialacademy.oitassist.competition.controller;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/stages")
@RequiredArgsConstructor
public class StageController {
    private final StageService stageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<StageResponse> createStage(
        @PathVariable Long competitionId,
        @Valid @RequestBody CreateStageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(stageService.create(competitionId, request));
    }
}
