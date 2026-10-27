package com.itasocialacademy.oitassist.competitionhistory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import lombok.Builder;

@Schema(description = "DTO representing a past competition in the user's participation history")
@Builder
public record CompetitionHistoryResponse(
    @Schema(description = "Unique identifier of the competition", example = "1") Long id,
    @Schema(description = "Title of the competition", example = "Всеукраїнська Олімпіада 2026") String title,
    @Schema(description = "Start date", example = "2026-09-01T09:00:00Z") ZonedDateTime dateStart,
    @Schema(description = "End date", example = "2026-12-25T18:00:00Z") ZonedDateTime dateFinish,
    @Schema(description = "Sum of the user's jury scores in the competition", example = "28.5") Double score,
    @Schema(description = "Sum of maximum points of all tasks in the competition", example = "45") Integer maxScore) {
}