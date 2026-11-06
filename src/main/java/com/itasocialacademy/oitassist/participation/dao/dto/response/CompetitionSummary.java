package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short information about the competition.")
public record CompetitionSummary(
    @Schema(description = "Competition ID", example = "1") Long id,
    @Schema(description = "Competition title", example = "Mathematics Olympiad 2026") String title) {
}