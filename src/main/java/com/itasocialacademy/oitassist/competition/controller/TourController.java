package com.itasocialacademy.oitassist.competition.controller;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
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
@RequestMapping("/api/v1/stages/{stageId}/tours")
@RequiredArgsConstructor
public class TourController {
    private final TourService tourService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<TourResponse> createTour(
        @PathVariable Long stageId,
        @Valid @RequestBody CreateTourRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tourService.create(stageId, request));
    }
}
