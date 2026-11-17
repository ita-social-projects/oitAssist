package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short information about the competition stage.")
public record StageSummary(
    @Schema(description = "Stage ID", example = "3") Long id,
    @Schema(description = "Stage title", example = "City stage") String title,
    @Schema(description = "Stage scope", example = "CITY") StageScope scope) {
}