package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "DTO representing a single application in the student's enrollment history.")
public record ApplicationHistoryItemResponse(
    @Schema(description = "Unique identifier of the application", example = "1") Long applicationId,
    @Schema(description = "Application creation date", example = "2026-06-07T09:50:30Z") Instant issuedAt,
    @Schema(description = "Current application status", example = "PENDING") RequestStatus status,
    CompetitionSummary competition,
    StageSummary stage) {
}