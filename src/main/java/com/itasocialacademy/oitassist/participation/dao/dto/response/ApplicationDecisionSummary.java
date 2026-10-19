package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "DTO representing the application's details in the list.")
public record ApplicationDecisionSummary(
    @Schema(description = "Unique identifier of the competition", example = "1") Long competitionId,
    @Schema(description = "Unique identifier of the stage", example = "1") Long stageId,
    @Schema(description = "ID of the user who processed the request", example = "5") Long processedBy,
    @Schema(description = "Request processing date", example = "2026-06-07T09:50:30Z") Instant processedAt) {
}
