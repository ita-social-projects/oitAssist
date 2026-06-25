package com.itasocialacademy.oitassist.competition.dao.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import lombok.Builder;

@Schema(description = "DTO representing a Competition entity response")
@Builder
public record CompetitionResponse(
    @Schema(description = "Unique identifier of the competition", example = "1")
    Long id,
    @Schema(description = "Title of the competition", example = "Всеукраїнська Олімпіада 2026")
    String title,
    @Schema(description = "Description of the competition")
    String description,
    @Schema(description = "Start date", example = "2026-09-01T09:00:00Z")
    ZonedDateTime dateStart,
    @Schema(description = "End date", example = "2026-12-25T18:00:00Z")
    ZonedDateTime dateFinish,
    @Schema(description = "Current lifecycle status", example = "PUBLISHED")
    CompetitionStatus status,
    @Schema(description = "ID of the user who created it", example = "5")
    Long createdBy,
    @Schema(description = "ID of the user who last updated it", example = "5")
    Long updatedBy) {
}