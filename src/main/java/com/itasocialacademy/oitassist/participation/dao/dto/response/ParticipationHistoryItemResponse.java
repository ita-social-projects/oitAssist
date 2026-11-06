package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO representing a single confirmed participation in the student's enrollment history.")
public record ParticipationHistoryItemResponse(
    @Schema(description = "Unique identifier of the participation record", example = "1") Long participationId,
    CompetitionSummary competition,
    StageSummary stage) {
}